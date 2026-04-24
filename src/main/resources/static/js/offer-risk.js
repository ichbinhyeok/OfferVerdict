(() => {
    const normalizeMode = (mode) => mode === "job_post" ? "job_post" : "offer_review";

    const initModeToggle = () => {
        const stage = document.querySelector(".decision-stage");
        if (!stage) {
            return;
        }

        const buttons = Array.from(document.querySelectorAll(".mode-pill[data-mode]"));
        const panels = Array.from(document.querySelectorAll("[data-mode-panel]"));
        const cards = Array.from(document.querySelectorAll("[data-mode-card]"));

        const syncMode = (nextMode) => {
            const mode = normalizeMode(nextMode);
            stage.dataset.activeMode = mode;

            buttons.forEach((button) => {
                const active = button.dataset.mode === mode;
                button.classList.toggle("active", active);
                button.setAttribute("aria-pressed", String(active));
            });

            panels.forEach((panel) => {
                const active = panel.dataset.modePanel === mode;
                panel.classList.toggle("active", active);
                panel.hidden = !active;
            });

            cards.forEach((card) => {
                const active = card.dataset.modeCard === mode;
                card.classList.toggle("active", active);
            });
        };

        const initialMode = normalizeMode(stage.dataset.activeMode);
        syncMode(initialMode);

        buttons.forEach((button) => {
            button.addEventListener("click", () => {
                const mode = normalizeMode(button.dataset.mode);
                syncMode(mode);

                const url = new URL(window.location.href);
                url.searchParams.set("mode", mode);
                window.history.replaceState({}, "", url);
            });
        });
    };

    const initIntentSignals = () => {
        const signalLinks = Array.from(document.querySelectorAll("[data-intent-signal]"));
        if (!signalLinks.length) {
            return;
        }

        signalLinks.forEach((link) => {
            link.addEventListener("click", () => {
                if (typeof window.gtag !== "function") {
                    return;
                }
                window.gtag("event", "intent_signal_click", {
                    intent: link.dataset.intent || "unknown",
                    analysis_mode: link.dataset.analysisMode || "offer_review",
                    verdict: link.dataset.verdict || "unknown"
                });
            });
        });
    };

    initModeToggle();
    initIntentSignals();
})();
