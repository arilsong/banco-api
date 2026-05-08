package com.caixa.api.controller;

import com.caixa.api.core.model.CoreUser;
import com.caixa.api.core.repository.CoreUserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/sdk")
public class SdkAuthorizationController {

    private final CoreUserRepository coreUserRepository;
    private final com.caixa.api.service.ThirdPartyService thirdPartyService;

    @GetMapping("/authorize")
    public String startSdkAuth(@RequestParam("consentRequestId") String consentRequestId, HttpSession session) {
        log.info("Iniciando autorização SDK Caixa para consentRequestId: {}", consentRequestId);
        session.setAttribute("sdkConsentRequestId", consentRequestId);
        
        CoreUser user = (CoreUser) session.getAttribute("sdkUser");
        if (user == null) {
            return "redirect:/sdk/login";
        }
        
        return "redirect:/sdk/confirm";
    }

    @GetMapping("/login")
    public String showSdkLogin(HttpSession session, Model model) {
        if (session.getAttribute("sdkConsentRequestId") == null) {
            return "redirect:/error";
        }
        return "sdk_login";
    }

    @PostMapping("/login")
    public String processSdkLogin(@RequestParam("username") String username,
                                 @RequestParam("password") String password,
                                 HttpSession session, Model model) {
        Optional<CoreUser> userOpt = coreUserRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            session.setAttribute("sdkUser", userOpt.get());
            return "redirect:/sdk/confirm";
        }

        model.addAttribute("error", "Credenciais de acesso Caixa inválidas.");
        return "sdk_login";
    }

    @GetMapping("/confirm")
    public String showSdkConfirm(HttpSession session, Model model) {
        CoreUser user = (CoreUser) session.getAttribute("sdkUser");
        String consentRequestId = (String) session.getAttribute("sdkConsentRequestId");
        
        if (user == null) return "redirect:/sdk/login";
        if (consentRequestId == null) return "redirect:/error";
        
        model.addAttribute("user", user);
        model.addAttribute("consentRequestId", consentRequestId);
        return "sdk_authorize";
    }

    @PostMapping("/confirm")
    public String processSdkConfirm(@RequestParam("action") String action, HttpSession session) {
        String consentRequestId = (String) session.getAttribute("sdkConsentRequestId");
        
        // Gerar um authToken (simulação de um token seguro/OTP)
        String authToken = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        log.info("Ação de autorização SDK Caixa: {} para ID: {}", action, consentRequestId);
        log.info("Token gerado (authToken): {}", authToken);
        
        String callbackUri = thirdPartyService.getCallbackUri(consentRequestId);
        if (callbackUri == null || callbackUri.isEmpty()) {
            log.warn("callbackUri não encontrado para o consentRequestId {}", consentRequestId);
            return "redirect:/error";
        }
        
        // Redireciona para o callbackUri do PISP/SDK, enviando o authToken
        return "redirect:" + callbackUri + "?consentRequestId=" + consentRequestId + "&authToken=" + authToken;
    }
}
