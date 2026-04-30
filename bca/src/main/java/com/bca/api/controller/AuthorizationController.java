package com.bca.api.controller;

import com.bca.api.core.model.CoreUser;
import com.bca.api.core.repository.CoreUserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {

    private final CoreUserRepository coreUserRepository;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam("consentId") String consentId,
                              @RequestParam("redirect_uri") String redirectUri,
                              Model model) {
        model.addAttribute("consentId", consentId);
        model.addAttribute("redirectUri", redirectUri);
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("consentId") String consentId,
                             @RequestParam("redirectUri") String redirectUri,
                             HttpSession session,
                             Model model) {
        log.info("Processando login para Open Banking: {}", username);
        Optional<CoreUser> userOpt = coreUserRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            session.setAttribute("user", userOpt.get());
            session.setAttribute("consentId", consentId);
            session.setAttribute("redirectUri", redirectUri);
            return "redirect:/authorize";
        }

        model.addAttribute("error", "Credenciais inválidas");
        model.addAttribute("consentId", consentId);
        model.addAttribute("redirectUri", redirectUri);
        return "login";
    }

    @GetMapping("/authorize")
    public String showAuthorizePage(HttpSession session, Model model) {
        CoreUser user = (CoreUser) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("consentId", session.getAttribute("consentId"));
        return "authorize";
    }

    @PostMapping("/authorize")
    public String processAuthorize(@RequestParam("action") String action,
                                 HttpSession session) {
        String redirectUri = (String) session.getAttribute("redirectUri");
        String consentId = (String) session.getAttribute("consentId");

        if ("confirm".equals(action)) {
            String authCode = UUID.randomUUID().toString();
            log.info("Consentimento confirmado para consentId: {}. Gerando auth_code: {}", consentId, authCode);
            return "redirect:" + redirectUri + "?consentId=" + consentId + "&auth_code=" + authCode;
        } else {
            log.info("Consentimento cancelado para consentId: {}", consentId);
            return "redirect:" + redirectUri + "?error=access_denied&consentId=" + consentId;
        }
    }
}
