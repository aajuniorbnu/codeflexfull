package com.codeflex.bilheteria.config;

import com.codeflex.bilheteria.models.*;
import com.codeflex.bilheteria.repositories.*;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoData implements ApplicationRunner {
  private final FilmeRepository filmes;
  private final SalaRepository salas;
  private final EventoRepository eventos;
  private final boolean demo;

  public DemoData(
      FilmeRepository f, SalaRepository s, EventoRepository e, @Value("${app.demo}") boolean demo) {
    filmes = f;
    salas = s;
    eventos = e;
    this.demo = demo;
  }

  @Transactional
  public void run(ApplicationArguments args) {
    if (!demo || filmes.count() > 0) return;
    String[][] dados = {
      {
        "693134",
        "Duna: Parte Dois",
        "O destino de um universo começa com uma escolha. Paul Atreides se une a Chani e aos Fremen"
            + " em uma jornada entre o amor e o futuro do império.",
        "166",
        "14",
        "Ficção científica, Aventura",
        "/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
        "/uhUO7vQQKvCTfQWubOt5MAKokbL.jpg",
        "8.2",
        "EM_CARTAZ"
      },
      {
        "157336",
        "Interestelar",
        "Uma equipe de exploradores atravessa um buraco de minhoca em busca de um novo lar para a"
            + " humanidade.",
        "169",
        "10",
        "Ficção científica, Drama",
        "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
        "/pbrkL804c8yAv3zBZR4QPEafpAR.jpg",
        "8.4",
        "EM_CARTAZ"
      },
      {
        "569094",
        "Homem-Aranha: Através do Aranhaverso",
        "Miles Morales reencontra Gwen Stacy e se aventura pelo multiverso, onde conhece uma equipe"
            + " de heróis que protege a própria existência.",
        "140",
        "10",
        "Animação, Aventura",
        "/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg",
        "/4HodYYKEIsGOdinkGi2Ucz6X9i0.jpg",
        "8.4",
        "EM_CARTAZ"
      },
      {
        "508883",
        "O Menino e a Garça",
        "Em um mundo compartilhado pelos vivos e pelos mortos, um menino descobre um novo começo.",
        "124",
        "12",
        "Animação, Fantasia",
        "/f4oZTcfGrVTXKTWg157AwikXqmP.jpg",
        "/ybn3jCia5XBD0ZgEM07gcUPuRNh.jpg",
        "7.5",
        "EM_CARTAZ"
      },
      {
        "27205",
        "A Origem",
        "Um especialista em extrair segredos dos sonhos recebe uma missão que pode mudar sua vida:"
            + " plantar uma ideia.",
        "148",
        "14",
        "Ficção científica, Suspense",
        "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
        "/s3TBrRGB1iav7gFOCNx3H31MoES.jpg",
        "8.4",
        "EM_BREVE"
      },
      {
        "129",
        "A Viagem de Chihiro",
        "Uma garota entra em um mundo de espíritos e precisa encontrar coragem para salvar sua"
            + " família.",
        "125",
        "L",
        "Animação, Fantasia",
        "/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg",
        "/6vCLt0a2mDAnMBeesnbobo8fTC6.jpg",
        "8.5",
        "EM_BREVE"
      }
    };
    int idx = 0;
    for (var d : dados) {
      var f = new Filme();
      f.tmdbId = Integer.valueOf(d[0]);
      f.titulo = d[1];
      f.sinopse = d[2];
      f.duracao = Integer.valueOf(d[3]);
      f.classificacao = d[4];
      f.generos = d[5];
      f.posterPath = d[6];
      f.backdropPath = d[7];
      f.nota = Double.valueOf(d[8]);
      f.categoria = d[9];
      f.demonstracao = true;
      f.atualizadoEm = LocalDateTime.now();
      f = filmes.save(f);
      var sala = new Sala();
      sala.nome = "Sala " + (++idx);
      sala.fileiras = 6;
      sala.colunas = 10;
      sala.formato = idx == 1 ? "IMAX" : "2D";
      sala = salas.save(sala);
      if ("EM_CARTAZ".equals(f.categoria))
        for (int day = 0; day < 5; day++)
          for (int hour : new int[] {14, 18, 22}) {
            var start = LocalDate.now().plusDays(day).atTime(hour, 0);
            if (start.isBefore(LocalDateTime.now())) continue;
            var e = new Evento();
            e.filme = f;
            e.sala = sala;
            e.nome = f.titulo;
            e.data = start.toLocalDate();
            e.inicio = start;
            e.fim = start.plusMinutes(f.duracao + 20);
            e.preco = new BigDecimal(idx == 1 ? "38.00" : "28.00");
            e.vagas = 60;
            e.idioma = hour == 22 ? "Legendado" : "Dublado";
            eventos.save(e);
          }
    }
  }
}
