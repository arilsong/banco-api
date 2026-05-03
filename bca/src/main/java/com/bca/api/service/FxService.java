package com.bca.api.service;

import com.bca.api.config.FxConfig;
import com.bca.api.dto.FxQuoteRequest;
import com.bca.api.dto.FxTransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FxService {

    private final FxConfig fxConfig;

    @Value("${fsp.currency:CVE}")
    private String fspCurrency;

    private final ConcurrentHashMap<String, FxReservation> reservations = new ConcurrentHashMap<>();

    private static class FxReservation {
        String commitRequestId;
        BigDecimal sourceAmount;
        String sourceCurrency;
        BigDecimal targetAmount;
        String targetCurrency;
        String fulfilment;
    }

    public Map<String, Object> processQuote(FxQuoteRequest request) {
        FxQuoteRequest.ConversionTerms terms = request.getConversionTerms();
        String sourceCurrency = terms.getSourceAmount().getCurrency();
        String targetCurrency = terms.getTargetAmount().getCurrency();

        BigDecimal rate = resolveRate(sourceCurrency, targetCurrency);
        BigDecimal sourceAmt = new BigDecimal(terms.getSourceAmount().getAmount());
        BigDecimal targetAmt = sourceAmt.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        log.info("FX Quote [BCA]: {} {} → {} {} (rate={})", sourceAmt, sourceCurrency, targetAmt, targetCurrency, rate);

        Map<String, Object> responseTerms = new HashMap<>();
        responseTerms.put("conversionId", terms.getConversionId());
        responseTerms.put("determiningTransferId", terms.getDeterminingTransferId());
        responseTerms.put("initiatingFsp", terms.getInitiatingFsp());
        responseTerms.put("counterPartyFsp", terms.getCounterPartyFsp());
        responseTerms.put("amountType", terms.getAmountType());
        responseTerms.put("sourceAmount", Map.of("currency", sourceCurrency, "amount", terms.getSourceAmount().getAmount()));
        responseTerms.put("targetAmount", Map.of("currency", targetCurrency, "amount", targetAmt.toPlainString()));
        responseTerms.put("expiration", terms.getExpiration() != null ? terms.getExpiration()
                : Instant.now().plusSeconds(30).toString());
        responseTerms.put("charges", new Object[0]);

        return Map.of(
                "conversionRequestId", request.getConversionRequestId(),
                "conversionTerms", responseTerms
        );
    }

    public Map<String, Object> reserveTransfer(FxTransferRequest request) {
        String commitRequestId = request.getCommitRequestId();
        String sourceCurrency  = request.getSourceAmount().getCurrency();
        String targetCurrency  = request.getTargetAmount().getCurrency();

        BigDecimal rate       = resolveRate(sourceCurrency, targetCurrency);
        BigDecimal sourceAmt  = new BigDecimal(request.getSourceAmount().getAmount());
        BigDecimal targetAmt  = sourceAmt.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        String fulfilment     = UUID.randomUUID().toString().replace("-", "");

        FxReservation reservation  = new FxReservation();
        reservation.commitRequestId = commitRequestId;
        reservation.sourceAmount    = sourceAmt;
        reservation.sourceCurrency  = sourceCurrency;
        reservation.targetAmount    = targetAmt;
        reservation.targetCurrency  = targetCurrency;
        reservation.fulfilment      = fulfilment;

        reservations.put(commitRequestId, reservation);
        log.info("FX Reservado [BCA]: {} {} → {} {} id={}", sourceAmt, sourceCurrency, targetAmt, targetCurrency, commitRequestId);

        return Map.of(
                "homeTransactionId", UUID.randomUUID().toString(),
                "fulfilment", fulfilment,
                "completedTimestamp", Instant.now().toString(),
                "conversionState", "RESERVED"
        );
    }

    public Map<String, Object> commitTransfer(String commitRequestId) {
        FxReservation reservation = reservations.remove(commitRequestId);
        if (reservation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva FX não encontrada: " + commitRequestId);
        }
        log.info("FX Confirmado [BCA]: id={}", commitRequestId);
        return Map.of(
                "completedTimestamp", Instant.now().toString(),
                "conversionState", "COMMITTED"
        );
    }

    public Map<String, Object> abortTransfer(String commitRequestId) {
        FxReservation reservation = reservations.remove(commitRequestId);
        if (reservation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva FX não encontrada: " + commitRequestId);
        }
        log.info("FX Abortado [BCA]: id={}", commitRequestId);
        return Map.of(
                "completedTimestamp", Instant.now().toString(),
                "conversionState", "ABORTED"
        );
    }

    private BigDecimal resolveRate(String sourceCurrency, String targetCurrency) {
        if (sourceCurrency.equals(fspCurrency)) {
            BigDecimal rate = fxConfig.getRates().get(targetCurrency);
            if (rate == null) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Taxa de câmbio não disponível para: " + targetCurrency);
            return BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP);
        } else {
            BigDecimal rate = fxConfig.getRates().get(sourceCurrency);
            if (rate == null) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Taxa de câmbio não disponível para: " + sourceCurrency);
            return rate;
        }
    }
}
