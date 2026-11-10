import React, { useState, useEffect, useRef } from "react";
import { createRoot } from "react-dom/client";
import {
  ArrowUpRight,
  ArrowRight,
  ArrowLeft,
  Ticket,
  Search,
  MapPin,
  ChevronDown,
  Clock,
  Play,
  X,
  Check,
  Armchair,
  ShieldCheck,
  Film,
  CalendarDays,
  Settings,
  LogOut,
  LoaderCircle,
  RefreshCw,
  Plus,
  Trash2,
  Menu,
  Star,
  Download,
  Pencil,
} from "lucide-react";
import QRCode from "qrcode";
import "./styles.css";
const money = (n) =>
  Number(n).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const time = (d) =>
  new Date(d).toLocaleTimeString("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  });
const day = (d) =>
  new Date(d).toLocaleDateString("pt-BR", { day: "2-digit", month: "short" });
const poster = (f, size = "w500") =>
  f?.poster_path?.startsWith("/")
    ? `https://image.tmdb.org/t/p/${size}${f.poster_path}`
    : "";
const backdrop = (f) =>
  f?.backdrop_path?.startsWith("/")
    ? `https://image.tmdb.org/t/p/original${f.backdrop_path}`
    : poster(f);
async function api(path, options = {}) {
  const r = await fetch("/api" + path, {
    ...options,
    headers: { "Content-Type": "application/json", ...options.headers },
  });
  const body = await r.text();
  let data;
  try {
    data = body ? JSON.parse(body) : null;
  } catch {
    throw new Error("O serviço não respondeu. Tente novamente em instantes.");
  }
  if (!r.ok)
    throw new Error(
      data?.mensagem ||
        data?.detail ||
        `Não foi possível concluir (${r.status}). Confira os dados.`,
    );
  return data;
}
const post = (url, data) =>
  api(url, { method: "POST", body: JSON.stringify(data) });
function Button({ children, busy, ...props }) {
  return (
    <button {...props} disabled={props.disabled || busy}>
      {busy ? <LoaderCircle className="spin" size={18} /> : null}
      {children}
    </button>
  );
}
function Poster({ filme }) {
  const [failed, setFailed] = useState(false);
  return failed || !poster(filme) ? (
    <div className="poster-fallback">
      <Film />
      <strong>{filme.titulo}</strong>
    </div>
  ) : (
    <img
      loading="lazy"
      src={poster(filme)}
      alt={`Cartaz de ${filme.titulo}`}
      onError={() => setFailed(true)}
    />
  );
}
function Modal({ children, onClose, wide = false, label }) {
  const ref = useRef();
  useEffect(() => {
    const prev = document.activeElement;
    const old = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    ref.current?.focus();
    const handler = (e) => {
      if (e.key === "Escape") onClose();
      if (e.key === "Tab") {
        const els = ref.current.querySelectorAll(
          'button:not(:disabled),input,select,a[href],textarea,[tabindex="0"]',
        );
        if (!els.length) return;
        const first = els[0],
          last = els[els.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener("keydown", handler);
    return () => {
      document.body.style.overflow = old;
      document.removeEventListener("keydown", handler);
      prev?.focus();
    };
  }, []);
  return (
    <div
      className="overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <section
        className={"modal " + (wide ? "wide" : "")}
        role="dialog"
        aria-modal="true"
        aria-label={label}
        tabIndex={-1}
        ref={ref}
      >
        <button className="close" aria-label="Fechar" onClick={onClose}>
          <X size={21} />
        </button>
        {children}
      </section>
    </div>
  );
}
function App() {
  const [movies, setMovies] = useState([]),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [query, setQuery] = useState(""),
    [genre, setGenre] = useState("Todos os gêneros"),
    [tab, setTab] = useState("EM_CARTAZ"),
    [selected, setSelected] = useState(null),
    [booking, setBooking] = useState(null),
    [ticket, setTicket] = useState(null),
    [page, setPage] = useState("home"),
    [menu, setMenu] = useState(false);
  async function load() {
    setLoading(true);
    try {
      setMovies(await api("/filmes"));
      setError("");
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    load();
    const params = new URLSearchParams(location.search);
    if (params.get("ingresso"))
      api("/ingressos/" + encodeURIComponent(params.get("ingresso")))
        .then(setTicket)
        .catch((e) => setError(e.message));
  }, []);
  const hero = movies.find((m) => m.categoria === "EM_CARTAZ") || movies[0];
  const filtered = movies.filter(
    (m) =>
      m.categoria === tab &&
      m.titulo.toLowerCase().includes(query.toLowerCase()) &&
      (genre === "Todos os gêneros" || m.generos?.includes(genre)),
  );
  const genres = [
    ...new Set(movies.flatMap((m) => m.generos?.split(", ") || [])),
  ];
  function tickets() {
    setPage("tickets");
    setMenu(false);
  }
  function purchased(t) {
    const stored = JSON.parse(localStorage.getItem("codeflex-tickets") || "[]");
    localStorage.setItem(
      "codeflex-tickets",
      JSON.stringify([...new Set([t.token, ...stored])]),
    );
    setBooking(null);
    setTicket(t);
  }
  return (
    <>
      <header className="header">
        <a
          href="#"
          className="brand"
          onClick={(e) => {
            e.preventDefault();
            setPage("home");
          }}
        >
          <span className="brand-icon">
            c<span>↗</span>
          </span>
          codeflex<span className="brand-dot">.</span>
        </a>
        <nav className={menu ? "mobile-open" : ""}>
          <button
            className={
              page === "home" && tab === "EM_CARTAZ" ? "nav-active" : ""
            }
            onClick={() => {
              setPage("home");
              setTab("EM_CARTAZ");
              setMenu(false);
            }}
          >
            Em cartaz
          </button>
          <button
            className={
              page === "home" && tab === "EM_BREVE" ? "nav-active" : ""
            }
            onClick={() => {
              setPage("home");
              setTab("EM_BREVE");
              setMenu(false);
              setTimeout(
                () =>
                  document
                    .querySelector("#filmes")
                    ?.scrollIntoView({ behavior: "smooth" }),
                20,
              );
            }}
          >
            Em breve
          </button>
          <button onClick={tickets}>Meus ingressos</button>
        </nav>
        <div className="header-right">
          <span className="location">
            <MapPin size={15} /> Codeflex Cinema <ChevronDown size={13} />
          </span>
          <button className="ticket-button" onClick={tickets}>
            <Ticket size={17} />
            <span>Meus ingressos</span>
          </button>
          <button
            className="mobile-toggle"
            aria-label="Abrir menu"
            onClick={() => setMenu(!menu)}
          >
            <Menu />
          </button>
        </div>
      </header>
      {page === "admin" ? (
        <Admin movies={movies} onRefresh={load} />
      ) : page === "tickets" ? (
        <MyTickets onTicket={setTicket} onExplore={() => setPage("home")} />
      ) : (
        <main>
          {hero && (
            <section
              className="hero"
              style={{
                backgroundImage: `linear-gradient(90deg,rgba(13,14,17,.98) 0%,rgba(13,14,17,.73) 38%,rgba(13,14,17,.08) 78%),linear-gradient(0deg,#111114 0%,transparent 45%),url("${backdrop(hero)}")`,
              }}
            >
              <div className="hero-inner">
                <div className="eyebrow">
                  <span className="live-dot" />
                  {hero.demonstracao
                    ? "MOSTRA ESPECIAL CODEFLEX"
                    : "AGORA NAS TELONAS"}
                </div>
                <h1>
                  {hero.titulo === "Duna: Parte Dois" ? (
                    <>
                      Uma história.
                      <br />
                      Um universo
                      <br />
                      <em>de sensações.</em>
                    </>
                  ) : (
                    <>
                      A próxima grande
                      <br />
                      história espera
                      <br />
                      <em>por você.</em>
                    </>
                  )}
                </h1>
                <div className="hero-movie">
                  <strong>{hero.titulo}</strong>
                  <span className="rating-age">{hero.classificacao}</span>
                  <span>{hero.duracao} min</span>
                  <span className="hero-format">IMERSÃO TOTAL</span>
                </div>
                <p>{hero.sinopse}</p>
                <div className="hero-actions">
                  <button className="primary" onClick={() => setSelected(hero)}>
                    Escolher minha sessão <ArrowUpRight size={19} />
                  </button>
                  {hero.trailer ? (
                    <a
                      className="outline"
                      href={`https://www.youtube.com/watch?v=${hero.trailer}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      <Play size={16} /> Ver trailer
                    </a>
                  ) : (
                    <button
                      className="outline"
                      onClick={() => setSelected(hero)}
                    >
                      <Play size={16} /> Conhecer o filme
                    </button>
                  )}
                </div>
                <span className="hero-note">
                  <ShieldCheck size={14} /> Seu próximo lugar favorito é aqui.
                </span>
              </div>
              <div className="hero-caption">
                <span>EM DESTAQUE</span>
                <strong>{hero.titulo}</strong>
                <div>
                  <i />
                  <i />
                  <i />
                </div>
              </div>
            </section>
          )}
          <section className="experience-strip">
            <span>
              <Film size={18} /> Histórias que merecem uma tela gigante
            </span>
            <span>
              <Armchair size={18} /> Seu lugar, do seu jeito
            </span>
            <span>
              <Ticket size={18} /> Da escolha ao ingresso, sem filas
            </span>
          </section>
          <section id="filmes" className="catalog section-wrap">
            <div className="section-top">
              <div>
                <span className="eyebrow muted">
                  LUZES, CÂMERA, SEU PRÓXIMO FILME
                </span>
                <h2>O que vamos assistir?</h2>
              </div>
              <span className="catalog-note">
                {movies.some((m) => m.demonstracao)
                  ? "Mostra de demonstração"
                  : "Catálogo atualizado pelo TMDB"}{" "}
                <span className="small-dot" />
              </span>
            </div>
            <div className="catalog-tools">
              <div className="tabs">
                <button
                  className={tab === "EM_CARTAZ" ? "active" : ""}
                  onClick={() => setTab("EM_CARTAZ")}
                >
                  Em cartaz{" "}
                  <span>
                    {movies.filter((m) => m.categoria === "EM_CARTAZ").length}
                  </span>
                </button>
                <button
                  className={tab === "EM_BREVE" ? "active" : ""}
                  onClick={() => setTab("EM_BREVE")}
                >
                  Em breve
                </button>
              </div>
              <div className="filters">
                <label className="search-field">
                  <Search size={17} />
                  <input
                    aria-label="Buscar filme"
                    placeholder="Encontre seu filme"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </label>
                <select
                  aria-label="Filtrar gênero"
                  value={genre}
                  onChange={(e) => setGenre(e.target.value)}
                >
                  <option>Todos os gêneros</option>
                  {genres.map((g) => (
                    <option key={g}>{g}</option>
                  ))}
                </select>
              </div>
            </div>
            {loading ? (
              <div className="loading">
                <LoaderCircle className="spin" /> Preparando a próxima sessão…
              </div>
            ) : error ? (
              <div className="empty">
                <Film />
                <h3>O cinema está nos bastidores</h3>
                <p>{error}</p>
                <button className="outline" onClick={load}>
                  Tentar novamente
                </button>
              </div>
            ) : !filtered.length ? (
              <div className="empty">
                <Search />
                <h3>Nenhum filme encontrado</h3>
                <p>Experimente outro título ou gênero.</p>
              </div>
            ) : (
              <div className="movie-grid">
                {filtered.map((m, i) => (
                  <article className="movie-card" key={m.id}>
                    <button
                      className="poster"
                      onClick={() => setSelected(m)}
                      aria-label={`Ver sessões de ${m.titulo}`}
                    >
                      <Poster filme={m} />
                      <span className="poster-badge">
                        {m.categoria === "EM_BREVE"
                          ? "EM BREVE"
                          : i === 0
                            ? "DESTAQUE"
                            : "EM CARTAZ"}
                      </span>
                      <span className="poster-cta">
                        {m.categoria === "EM_BREVE"
                          ? "Conhecer o filme"
                          : "Ver sessões"}{" "}
                        <ArrowUpRight size={18} />
                      </span>
                    </button>
                    <div className="movie-meta">
                      <span className={"age age-" + m.classificacao}>
                        {m.classificacao}
                      </span>
                      <span>{m.generos?.split(", ")[0]}</span>
                      <span>•</span>
                      <span>{m.duracao} min</span>
                    </div>
                    <button
                      className="movie-title"
                      onClick={() => setSelected(m)}
                    >
                      {m.titulo}
                    </button>
                    <div className="movie-bottom">
                      <span>
                        <Star size={13} /> {m.nota?.toFixed(1)}{" "}
                        <small>TMDB</small>
                      </span>
                      <button
                        onClick={() => setSelected(m)}
                        aria-label={`Abrir ${m.titulo}`}
                      >
                        <ArrowUpRight size={18} />
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
          <section className="cinema-banner section-wrap">
            <div className="banner-symbol">
              <Ticket size={65} strokeWidth={1} />
            </div>
            <div>
              <span className="eyebrow">MAIS CINEMA. MENOS COMPLICAÇÃO.</span>
              <h2>
                A melhor parte?
                <br />
                Seu lugar já está esperando.
              </h2>
              <p>
                Escolha o filme, encontre a sessão e reserve o seu assento.
                <br />O resto é deixar a história acontecer.
              </p>
            </div>
            <button
              className="light-button"
              onClick={() =>
                document
                  .querySelector("#filmes")
                  ?.scrollIntoView({ behavior: "smooth" })
              }
            >
              Encontrar meu filme <ArrowUpRight size={19} />
            </button>
            <span className="banner-circle" />
          </section>
        </main>
      )}
      <footer className="footer">
        <div>
          <a href="#" className="brand" onClick={() => setPage("home")}>
            codeflex<span className="brand-dot">.</span>
          </a>
          <p>O cinema começa com uma boa escolha.</p>
        </div>
        <div>
          <span>FEITO PARA QUEM AMA CINEMA</span>
          <p>
            Este produto usa a API do TMDB, mas não é endossado
            <br />
            ou certificado pelo TMDB.
          </p>
          <a
            href="https://www.themoviedb.org"
            target="_blank"
            rel="noreferrer"
            className="tmdb"
          >
            TMDB <span />
          </a>
        </div>
        <button className="admin-link" onClick={() => setPage("admin")}>
          <Settings size={14} /> Administração
        </button>
      </footer>
      {selected && (
        <MovieDetails
          movie={selected}
          onClose={() => setSelected(null)}
          onSession={(s) => {
            setSelected(null);
            setBooking(s);
          }}
        />
      )}
      {booking && (
        <Booking
          session={booking}
          onClose={() => setBooking(null)}
          onSuccess={purchased}
        />
      )}
      {ticket && (
        <TicketModal ticket={ticket} onClose={() => setTicket(null)} />
      )}
    </>
  );
}
function MovieDetails({ movie, onClose, onSession }) {
  const [sessions, setSessions] = useState([]),
    [date, setDate] = useState(""),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true);
  useEffect(() => {
    api(`/filmes/${movie.id}/sessoes`)
      .then((s) => {
        setSessions(s);
        setDate(s[0]?.data || "");
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [movie.id]);
  const dates = [...new Set(sessions.map((s) => s.data))];
  return (
    <Modal wide onClose={onClose} label={movie.titulo}>
      <div className="detail-layout">
        <div className="detail-poster">
          <Poster filme={movie} />
        </div>
        <div className="detail-content">
          <span className="eyebrow">
            {movie.categoria === "EM_BREVE"
              ? "EM BREVE"
              : "SUA PRÓXIMA HISTÓRIA"}
          </span>
          <h2>{movie.titulo}</h2>
          <div className="detail-meta">
            <span className="age">{movie.classificacao}</span>
            <span>
              <Clock size={14} /> {movie.duracao} min
            </span>
            <span>
              <Star size={14} /> {movie.nota?.toFixed(1)}
            </span>
          </div>
          <p className="genres">{movie.generos}</p>
          <p>{movie.sinopse}</p>
          {movie.elenco && (
            <p className="cast">
              <b>Elenco</b> {movie.elenco}
            </p>
          )}
          {movie.trailer && (
            <a
              className="trailer-link"
              href={`https://www.youtube.com/watch?v=${movie.trailer}`}
              target="_blank"
              rel="noreferrer"
            >
              <Play size={14} /> Assistir ao trailer
            </a>
          )}
          <div className="sessions">
            <h3>Escolha sua sessão</h3>
            {loading ? (
              <LoaderCircle className="spin" />
            ) : error ? (
              <p role="alert">{error}</p>
            ) : !sessions.length ? (
              <p>A programação deste filme será anunciada em breve.</p>
            ) : (
              <>
                <div className="date-tabs">
                  {dates.map((d) => (
                    <button
                      key={d}
                      className={date === d ? "selected" : ""}
                      onClick={() => setDate(d)}
                    >
                      {new Date(d + "T12:00:00").toLocaleDateString("pt-BR", {
                        weekday: "short",
                      })}
                      <strong>{day(d + "T12:00:00")}</strong>
                    </button>
                  ))}
                </div>
                <div className="session-list">
                  {sessions
                    .filter((s) => s.data === date)
                    .map((s) => (
                      <button
                        disabled={s.vagas < 1}
                        key={s.id}
                        onClick={() => onSession(s)}
                      >
                        <strong>{time(s.inicio)}</strong>
                        <span>
                          {s.sala?.formato} · {s.idioma}
                        </span>
                        <small>
                          {s.sala?.nome} · {money(s.preco)}
                        </small>
                        <ArrowUpRight size={16} />
                      </button>
                    ))}
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
}
function Booking({ session, onClose, onSuccess }) {
  const [map, setMap] = useState(null),
    [seats, setSeats] = useState([]),
    [reserve, setReserve] = useState(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [cpf, setCpf] = useState(""),
    [card, setCard] = useState(""),
    [now, setNow] = useState(Date.now());
  const reserveRef = useRef(null);
  useEffect(() => {
    api(`/eventos/${session.id}/assentos`)
      .then(setMap)
      .catch((e) => setError(e.message));
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => {
      clearInterval(timer);
      if (reserveRef.current)
        api("/reservas/" + reserveRef.current, { method: "DELETE" }).catch(
          () => {},
        );
    };
  }, [session.id]);
  const remaining = reserve
    ? Math.max(0, Math.floor((new Date(reserve.expira_em) - now) / 1000))
    : 0;
  async function next() {
    setBusy(true);
    setError("");
    try {
      const r = await post("/reservas", {
        evento_id: session.id,
        assentos: seats,
      });
      reserveRef.current = r.token;
      setReserve(r);
    } catch (e) {
      setError(e.message);
      api(`/eventos/${session.id}/assentos`)
        .then(setMap)
        .catch(() => {});
    } finally {
      setBusy(false);
    }
  }
  async function pay(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const t = await post("/checkout", {
        reserva_token: reserve.token,
        cpf: cpf.replace(/\D/g, ""),
        cartao_final: card,
      });
      reserveRef.current = null;
      onSuccess(t);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function back() {
    if (reserve)
      await api("/reservas/" + reserve.token, { method: "DELETE" }).catch(
        () => {},
      );
    reserveRef.current = null;
    setReserve(null);
    setSeats([]);
    setError("");
    api(`/eventos/${session.id}/assentos`)
      .then(setMap)
      .catch((e) => setError(e.message));
  }
  return (
    <Modal
      wide
      label="Reserva e pagamento"
      onClose={() => {
        if (!busy) onClose();
      }}
    >
      <div className="booking-head">
        <span className="eyebrow">
          {reserve ? "02 / FINALIZAR" : "01 / SEU LUGAR NA HISTÓRIA"}
        </span>
        <h2>
          {reserve ? "Falta pouco para o filme." : "Escolha o melhor lugar."}
        </h2>
        <p>
          {session.nome} · {day(session.inicio)} às {time(session.inicio)} ·{" "}
          {session.sala?.nome}
        </p>
      </div>
      <div className="booking-layout">
        <div className="booking-main">
          {reserve ? (
            <form onSubmit={pay} className="checkout">
              <div
                className={"reservation-timer " + (!remaining ? "expired" : "")}
              >
                <Clock size={17} />
                {remaining
                  ? `Seus assentos estão reservados por ${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, "0")}`
                  : "Sua reserva expirou. Escolha novamente."}
              </div>
              <label>
                CPF
                <input
                  required
                  inputMode="numeric"
                  pattern="[0-9]{11}"
                  maxLength={11}
                  placeholder="Apenas os 11 números"
                  value={cpf}
                  onChange={(e) => setCpf(e.target.value.replace(/\D/g, ""))}
                />
              </label>
              <label>
                Últimos 4 dígitos do cartão
                <input
                  required
                  inputMode="numeric"
                  pattern="[0-9]{4}"
                  maxLength={4}
                  placeholder="4242"
                  value={card}
                  onChange={(e) => setCard(e.target.value.replace(/\D/g, ""))}
                />
              </label>
              <p className="payment-note">
                <ShieldCheck size={18} /> Pagamento simulado para demonstração.
                Não informe o número completo do cartão. O final 0000 simula uma
                recusa.
              </p>
              <Button
                className="primary full"
                busy={busy}
                disabled={!remaining}
                type="submit"
              >
                Confirmar pagamento · {money(session.preco * seats.length)}{" "}
                <ArrowRight size={17} />
              </Button>
              <button
                type="button"
                className="text-button"
                onClick={back}
                disabled={busy}
              >
                <ArrowLeft size={14} /> Alterar assentos
              </button>
            </form>
          ) : (
            <>
              <div className="screen">
                <span>T E L A</span>
              </div>
              {!map ? (
                <div className="loading">
                  <LoaderCircle className="spin" />
                </div>
              ) : (
                <div className="seat-scroll">
                  <div
                    className="seat-map"
                    style={{
                      gridTemplateColumns: `24px repeat(${session.sala.colunas}, 1fr)`,
                    }}
                  >
                    {Array.from({ length: session.sala.fileiras }, (_, r) => (
                      <React.Fragment key={r}>
                        <span className="row-label">
                          {String.fromCharCode(65 + r)}
                        </span>
                        {Array.from(
                          { length: session.sala.colunas },
                          (_, c) => {
                            const key = String.fromCharCode(65 + r) + (c + 1),
                              occupied = map.ocupados.includes(key),
                              chosen = seats.includes(key);
                            return (
                              <button
                                key={key}
                                aria-label={`Assento ${key}${occupied ? " ocupado" : ""}`}
                                aria-pressed={chosen}
                                disabled={occupied}
                                className={
                                  "seat " +
                                  (chosen ? "chosen" : "") +
                                  (occupied ? " occupied" : "")
                                }
                                onClick={() =>
                                  setSeats(
                                    chosen
                                      ? seats.filter((s) => s !== key)
                                      : seats.length < 8
                                        ? [...seats, key]
                                        : seats,
                                  )
                                }
                              >
                                {c + 1}
                              </button>
                            );
                          },
                        )}
                      </React.Fragment>
                    ))}
                  </div>
                </div>
              )}
              <div className="seat-legend">
                <span>
                  <i />
                  Disponível
                </span>
                <span>
                  <i className="chosen" />
                  Selecionado
                </span>
                <span>
                  <i className="occupied" />
                  Ocupado
                </span>
              </div>
              <p className="seat-limit">
                Escolha até 8 assentos. A reserva dura 5 minutos.
              </p>
            </>
          )}
          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}
        </div>
        <aside className="order-summary">
          <span className="eyebrow muted">SUA SESSÃO</span>
          <div className="summary-movie">
            <div>
              <Poster filme={session.filme} />
            </div>
            <h3>
              {session.nome}
              <small>
                {session.sala?.formato} · {session.idioma}
              </small>
            </h3>
          </div>
          <dl>
            <div>
              <dt>Data e horário</dt>
              <dd>
                {day(session.inicio)} · {time(session.inicio)}
              </dd>
            </div>
            <div>
              <dt>Sala</dt>
              <dd>{session.sala?.nome}</dd>
            </div>
            <div>
              <dt>Assentos</dt>
              <dd>{seats.join(", ") || "Ainda não selecionados"}</dd>
            </div>
            <div>
              <dt>Ingresso inteiro</dt>
              <dd>{money(session.preco)}</dd>
            </div>
          </dl>
          <div className="total">
            <span>Total</span>
            <strong>{money(session.preco * seats.length)}</strong>
          </div>
          {!reserve && (
            <Button
              className="primary full"
              busy={busy}
              disabled={!seats.length}
              onClick={next}
            >
              Reservar e continuar <ArrowRight size={17} />
            </Button>
          )}
          <small className="secure">
            <ShieldCheck size={13} /> Sem taxas escondidas.
          </small>
        </aside>
      </div>
    </Modal>
  );
}
function TicketModal({ ticket, onClose }) {
  const [qr, setQr] = useState("");
  useEffect(() => {
    QRCode.toDataURL(`${location.origin}/?ingresso=${ticket.token}`, {
      width: 240,
      margin: 2,
      color: { dark: "#171719" },
    }).then(setQr);
  }, [ticket.token]);
  return (
    <Modal onClose={onClose} label="Ingresso confirmado">
      <div className="ticket-confirm">
        <span className="success-icon">
          <Check size={28} />
        </span>
        <span className="eyebrow">INGRESSO CONFIRMADO</span>
        <h2>A próxima cena é sua.</h2>
        <p>Seu lugar está garantido. Bom filme!</p>
        <div className="ticket-paper">
          <div className="ticket-top">
            <strong>codeflex.</strong>
            <span>{ticket.status}</span>
          </div>
          <h3>{ticket.evento.nome}</h3>
          <p>
            {ticket.evento.sala?.nome} ·{" "}
            {ticket.evento.idioma || "Sessão especial"}
          </p>
          <div className="ticket-data">
            <span>
              DATA
              <strong>{day(ticket.evento.inicio || ticket.evento.data)}</strong>
            </span>
            <span>
              HORÁRIO
              <strong>
                {ticket.evento.inicio ? time(ticket.evento.inicio) : "—"}
              </strong>
            </span>
            <span>
              ASSENTOS
              <strong>
                {ticket.assentos || ticket.quantidade + " ingressos"}
              </strong>
            </span>
          </div>
          <div className="ticket-perf" />
          {qr && (
            <img
              className="qr"
              src={qr}
              alt="QR Code para consultar este ingresso"
            />
          )}
          <small>
            Pedido #{String(ticket.id).padStart(5, "0")} · {money(ticket.valor)}
          </small>
          <small>Guarde este código. Ele dá acesso ao seu ingresso.</small>
        </div>
        <button className="primary full" onClick={() => window.print()}>
          <Download size={17} /> Imprimir / salvar em PDF
        </button>
      </div>
    </Modal>
  );
}
function MyTickets({ onTicket, onExplore }) {
  const [items, setItems] = useState([]),
    [loading, setLoading] = useState(true),
    [error, setError] = useState("");
  useEffect(() => {
    const tokens = JSON.parse(localStorage.getItem("codeflex-tickets") || "[]");
    Promise.all(tokens.map((t) => api("/ingressos/" + encodeURIComponent(t))))
      .then(setItems)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);
  return (
    <main className="section-wrap tickets-page">
      <span className="eyebrow">SUAS PRÓXIMAS HISTÓRIAS</span>
      <h1>Meus ingressos</h1>
      <p className="muted">
        Compras realizadas neste navegador. Guarde o QR Code para acessar de
        outro aparelho.
      </p>
      {loading ? (
        <LoaderCircle className="spin" />
      ) : error ? (
        <div className="error">{error}</div>
      ) : !items.length ? (
        <div className="empty">
          <Ticket size={40} />
          <h3>Uma boa história está esperando.</h3>
          <p>Você ainda não comprou ingressos neste navegador.</p>
          <button className="primary" onClick={onExplore}>
            Explorar filmes <ArrowRight size={16} />
          </button>
        </div>
      ) : (
        <div className="tickets-grid">
          {items.map((i) => (
            <button
              className="ticket-list-card"
              key={i.id}
              onClick={() => onTicket(i)}
            >
              <Ticket />
              <div>
                <strong>{i.evento.nome}</strong>
                <span>
                  {day(i.evento.inicio || i.evento.data)} ·{" "}
                  {i.assentos || i.quantidade + " ingressos"}
                </span>
              </div>
              <span className="paid">PAGO</span>
              <ArrowUpRight />
            </button>
          ))}
        </div>
      )}
    </main>
  );
}
function Admin({ movies, onRefresh }) {
  const [token, setToken] = useState(""),
    [key, setKey] = useState(""),
    [data, setData] = useState(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [search, setSearch] = useState(""),
    [results, setResults] = useState([]),
    [notice, setNotice] = useState(""),
    [dateFilter, setDateFilter] = useState(""),
    [editRoom, setEditRoom] = useState(null),
    [editSession, setEditSession] = useState(null);
  const call = (url, opts = {}) =>
    api("/admin" + url, {
      ...opts,
      headers: { "X-Admin-Token": key || token, ...opts.headers },
    });
  async function load() {
    const [rooms, sessions, reports, tmdb] = await Promise.all([
      call("/salas"),
      call("/sessoes"),
      call("/relatorios"),
      call("/tmdb/status"),
    ]);
    setData({ rooms, sessions, reports, tmdb });
  }
  async function action(fn, message) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await fn();
      await load();
      onRefresh();
      if (message) setNotice(message);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function login(e) {
    e.preventDefault();
    setBusy(true);
    try {
      await load();
      setKey(token);
      setError("");
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  function formData(e) {
    return Object.fromEntries(new FormData(e.target).entries());
  }
  return (
    <main className="section-wrap admin-page">
      <span className="eyebrow">BASTIDORES CODEFLEX</span>
      <div className="section-top">
        <h1>Administração</h1>
        {key && (
          <button
            className="outline"
            onClick={() => {
              setKey("");
              setToken("");
              setData(null);
            }}
          >
            <LogOut size={15} /> Sair
          </button>
        )}
      </div>
      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}
      {notice && <div className="notice">{notice}</div>}
      {!key ? (
        <form className="admin-login panel" onSubmit={login}>
          <ShieldCheck size={28} />
          <h3>Acesso aos bastidores</h3>
          <p>Informe a chave administrativa configurada no servidor.</p>
          <label>
            Chave de acesso
            <input
              type="password"
              required
              value={token}
              onChange={(e) => setToken(e.target.value)}
              autoComplete="off"
            />
          </label>
          <Button className="primary" busy={busy}>
            Entrar <ArrowRight size={16} />
          </Button>
        </form>
      ) : (
        data && (
          <>
            <div className="stat-grid">
              <div>
                <span>Faturamento</span>
                <strong>{money(data.reports.faturamento)}</strong>
              </div>
              <div>
                <span>Ingressos vendidos</span>
                <strong>{data.reports.ingressos}</strong>
              </div>
              <div>
                <span>Pedidos</span>
                <strong>{data.reports.pedidos}</strong>
              </div>
              <div>
                <span>Salas</span>
                <strong>{data.rooms.length}</strong>
              </div>
            </div>
            <section className="panel">
              <div className="section-top">
                <div>
                  <h2>Catálogo de filmes</h2>
                  <p>
                    TMDB:{" "}
                    {data.tmdb.configurado
                      ? "conectado"
                      : "credencial não configurada"}{" "}
                    · {data.tmdb.situacao}
                  </p>
                </div>
                <Button
                  className="outline"
                  busy={busy}
                  disabled={!data.tmdb.configurado}
                  onClick={() =>
                    action(
                      () => call("/tmdb/sincronizar", { method: "POST" }),
                      "Catálogo atualizado. Cadastre as sessões dos filmes importados.",
                    )
                  }
                >
                  <RefreshCw size={16} /> Atualizar em cartaz / em breve
                </Button>
              </div>
              <form
                className="inline-form"
                onSubmit={(e) => {
                  e.preventDefault();
                  action(async () =>
                    setResults(
                      await call(
                        "/tmdb/buscar?titulo=" + encodeURIComponent(search),
                      ),
                    ),
                  );
                }}
              >
                <input
                  required
                  placeholder="Buscar filme no TMDB"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                <Button
                  className="primary"
                  busy={busy}
                  disabled={!data.tmdb.configurado}
                >
                  Buscar
                </Button>
              </form>
              <div className="search-results">
                {results.slice(0, 8).map((f) => (
                  <div key={f.id}>
                    <span>
                      {f.title} <small>{f.release_date}</small>
                    </span>
                    <Button
                      className="outline"
                      busy={busy}
                      onClick={() =>
                        action(
                          () =>
                            call("/tmdb/importar/" + f.id, { method: "POST" }),
                          "Filme importado.",
                        )
                      }
                    >
                      <Plus size={14} /> Importar
                    </Button>
                  </div>
                ))}
              </div>
            </section>
            <div className="admin-columns">
              <section className="panel">
                <h2>{editRoom ? "Editar sala" : "Nova sala"}</h2>
                <form
                  key={editRoom?.id || "nova-sala"}
                  onSubmit={(e) => {
                    e.preventDefault();
                    const d = formData(e);
                    action(
                      () =>
                        call("/salas" + (editRoom ? "/" + editRoom.id : ""), {
                          method: editRoom ? "PUT" : "POST",
                          body: JSON.stringify({
                            ...d,
                            fileiras: +d.fileiras,
                            colunas: +d.colunas,
                          }),
                        }).then(() => setEditRoom(null)),
                      "Sala cadastrada.",
                    );
                  }}
                >
                  <label>
                    Nome
                    <input
                      name="nome"
                      placeholder="Sala 7"
                      defaultValue={editRoom?.nome || ""}
                      required
                    />
                  </label>
                  <div className="form-row">
                    <label>
                      Fileiras
                      <input
                        name="fileiras"
                        type="number"
                        min="1"
                        max="20"
                        defaultValue={editRoom?.fileiras || 6}
                        required
                      />
                    </label>
                    <label>
                      Assentos por fileira
                      <input
                        name="colunas"
                        type="number"
                        min="1"
                        max="20"
                        defaultValue={editRoom?.colunas || 10}
                        required
                      />
                    </label>
                  </div>
                  <label>
                    Formato
                    <select
                      name="formato"
                      defaultValue={editRoom?.formato || "2D"}
                    >
                      <option>2D</option>
                      <option>3D</option>
                      <option>IMAX</option>
                    </select>
                  </label>
                  <Button className="primary" busy={busy}>
                    <Plus size={15} />{" "}
                    {editRoom ? "Salvar alterações" : "Criar sala e planta"}
                  </Button>
                  {editRoom && (
                    <button
                      type="button"
                      className="text-button"
                      onClick={() => setEditRoom(null)}
                    >
                      Cancelar edição
                    </button>
                  )}
                </form>
                <div className="room-list">
                  {data.rooms.map((r) => (
                    <div key={r.id}>
                      <span>
                        {r.nome} · {r.fileiras * r.colunas} lugares ·{" "}
                        {r.formato}
                      </span>
                      <button
                        aria-label={`Editar ${r.nome}`}
                        onClick={() => setEditRoom(r)}
                      >
                        <Pencil size={15} />
                      </button>
                      <button
                        aria-label={`Desativar ${r.nome}`}
                        disabled={busy || !r.ativa}
                        onClick={() =>
                          action(
                            () => call("/salas/" + r.id, { method: "DELETE" }),
                            "Sala desativada.",
                          )
                        }
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  ))}
                </div>
              </section>
              <section className="panel">
                <h2>{editSession ? "Editar sessão" : "Programar sessão"}</h2>
                <form
                  key={editSession?.id || "nova-sessao"}
                  onSubmit={(e) => {
                    e.preventDefault();
                    const d = formData(e);
                    action(
                      () =>
                        call(
                          "/sessoes" +
                            (editSession ? "/" + editSession.id : ""),
                          {
                            method: editSession ? "PUT" : "POST",
                            body: JSON.stringify({
                              ...d,
                              filme_id: +d.filme_id,
                              sala_id: +d.sala_id,
                              preco: +d.preco,
                            }),
                          },
                        ).then(() => setEditSession(null)),
                      "Sessão programada.",
                    );
                  }}
                >
                  <label>
                    Filme
                    <select
                      name="filme_id"
                      defaultValue={editSession?.filme?.id}
                      required
                    >
                      {movies.map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.titulo}
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="form-row">
                    <label>
                      Sala
                      <select
                        name="sala_id"
                        defaultValue={editSession?.sala?.id}
                        required
                      >
                        {data.rooms
                          .filter((r) => r.ativa)
                          .map((r) => (
                            <option key={r.id} value={r.id}>
                              {r.nome}
                            </option>
                          ))}
                      </select>
                    </label>
                    <label>
                      Idioma
                      <select name="idioma" defaultValue={editSession?.idioma}>
                        <option>Dublado</option>
                        <option>Legendado</option>
                        <option>Nacional</option>
                      </select>
                    </label>
                  </div>
                  <label>
                    Início
                    <input
                      name="inicio"
                      type="datetime-local"
                      defaultValue={editSession?.inicio?.slice(0, 16)}
                      required
                    />
                  </label>
                  <label>
                    Preço inteiro (R$)
                    <input
                      name="preco"
                      type="number"
                      min="0.01"
                      step="0.01"
                      defaultValue={editSession?.preco || 28}
                      required
                    />
                  </label>
                  <Button className="primary" busy={busy}>
                    <CalendarDays size={16} />{" "}
                    {editSession ? "Salvar alterações" : "Programar sessão"}
                  </Button>
                  {editSession && (
                    <button
                      type="button"
                      className="text-button"
                      onClick={() => setEditSession(null)}
                    >
                      Cancelar edição
                    </button>
                  )}
                  <p className="muted">
                    O intervalo de limpeza de 20 minutos é considerado ao
                    verificar a disponibilidade da sala.
                  </p>
                </form>
              </section>
            </div>
            <section className="panel">
              <div className="section-top">
                <h2>Agenda de sessões</h2>
                <input
                  type="date"
                  aria-label="Filtrar data das sessões"
                  value={dateFilter}
                  onChange={(e) => setDateFilter(e.target.value)}
                />
              </div>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Filme</th>
                      <th>Data</th>
                      <th>Sala</th>
                      <th>Ocupação</th>
                      <th>Preço</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {data.sessions
                      .filter(
                        (s) =>
                          s.ativa && (!dateFilter || s.data === dateFilter),
                      )
                      .sort((a, b) =>
                        (a.inicio || "").localeCompare(b.inicio || ""),
                      )

                      .map((s) => (
                        <tr key={s.id}>
                          <td>{s.nome}</td>
                          <td>
                            {s.inicio
                              ? `${day(s.inicio)} · ${time(s.inicio)}`
                              : s.data}
                          </td>
                          <td>{s.sala?.nome || "Evento"}</td>
                          <td>
                            {s.sala
                              ? `${s.sala.fileiras * s.sala.colunas - s.vagas}/${s.sala.fileiras * s.sala.colunas}`
                              : s.vagas + " livres"}
                          </td>
                          <td>{money(s.preco)}</td>
                          <td>
                            <button
                              aria-label={`Editar sessão ${s.id}`}
                              onClick={() => setEditSession(s)}
                            >
                              <Pencil size={15} />
                            </button>
                            <button
                              aria-label={`Cancelar sessão ${s.id}`}
                              disabled={busy}
                              onClick={() =>
                                action(
                                  () =>
                                    call("/sessoes/" + s.id, {
                                      method: "DELETE",
                                    }),
                                  "Sessão cancelada.",
                                )
                              }
                            >
                              <Trash2 size={15} />
                            </button>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </section>
            <div className="admin-columns">
              <section className="panel">
                <h2>Filmes mais vendidos</h2>
                {Object.entries(data.reports.por_filme)
                  .sort((a, b) => b[1] - a[1])
                  .map(([title, n]) => (
                    <div className="report-line" key={title}>
                      <span>{title}</span>
                      <strong>{n} ingressos</strong>
                    </div>
                  ))}
                {!data.reports.pedidos && <p>Ainda não há vendas.</p>}
              </section>
              <section className="panel">
                <h2>Faturamento por dia</h2>
                {Object.entries(data.reports.por_dia).map(([date, n]) => (
                  <div className="report-line" key={date}>
                    <span>{date}</span>
                    <strong>{money(n)}</strong>
                  </div>
                ))}
                {!data.reports.pedidos && <p>Ainda não há vendas.</p>}
              </section>
            </div>
          </>
        )
      )}
    </main>
  );
}
createRoot(document.getElementById("root")).render(<App />);
