package com.codeflex.bilheteria.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminFilter extends OncePerRequestFilter {
  private final String token;

  public AdminFilter(@Value("${admin.token:}") String token) {
    this.token = token;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (req.getRequestURI().startsWith("/admin")) {
      String supplied = req.getHeader("X-Admin-Token");
      if (token.isBlank()
          || supplied == null
          || !MessageDigest.isEqual(
              token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
        res.setStatus(401);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter()
            .write(
                "{\"codigo\":\"nao_autorizado\",\"mensagem\":\"Informe uma chave administrativa"
                    + " válida\",\"detalhes\":[]}");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
