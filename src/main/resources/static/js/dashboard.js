/* 
 * OfferVerdict Dashboard Logic 2026
 * Handles simulator sliders, presets, and chart updates
 */

document.addEventListener('DOMContentLoaded', () => {
    // State
    const state = {
        salary: parseInt(document.getElementById('salary-slider').value) || 120000,
        contribution401k: 5,
        simulations: 1
    };

    // Elements
    const salarySlider = document.getElementById('salary-slider');
    const salaryVal = document.getElementById('salary-val');
    const slider401k = document.getElementById('401k-slider');
    const val401k = document.getElementById('401k-val');
    const presets = document.querySelectorAll('.preset-btn');
    const progressBar = document.getElementById('progress-fill');
    const progressMsg = document.getElementById('progress-msg');

    // --- INIT ---
    updateUI();

    // --- EVENTS ---
    salarySlider.addEventListener('input', (e) => {
        state.salary = parseInt(e.target.value);
        updateUI();
    });

    slider401k.addEventListener('input', (e) => {
        state.contribution401k = parseInt(e.target.value);
        val401k.textContent = state.contribution401k + '%';
        updateRetirementImpact();
    });

    presets.forEach(btn => {
        btn.addEventListener('click', () => {
            presets.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            handlePreset(btn.dataset.scenario);
        });
    });

    // --- CORE LOGIC ---
    function updateUI() {
        // 1. Update Slider Text
        salaryVal.textContent = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            maximumFractionDigits: 0
        }).format(state.salary);

        // 2. Simulate Recalculation (Client-side illusion)
        // In real app, this would debounce and hit /api/calculate
        const gain = (state.salary * 0.75) - (120000 * 0.70); // Mock gain logic
        const gainStr = gain > 0 ? '+' + Math.round(gain / 12) : Math.round(gain / 12);

        // Only update if significative change (simulated)
        // document.getElementById('net-gain-display').textContent = '$' + gainStr; 

        // 3. Update Progress Bar
        // Goal: $200k = 100%, $80k = 0%
        const progress = Math.min(100, Math.max(0, (state.salary - 80000) / (200000 - 80000) * 100));
        progressBar.style.width = progress + '%';

        if (progress > 80) {
            progressBar.style.background = 'var(--success-gradient)';
            progressMsg.textContent = "🔥 Strong GO Verdict! You're winning.";
            document.getElementById('verdict-badge').textContent = "GO";
            document.getElementById('verdict-badge').className = "verdict-badge verdict-go";
        } else if (progress > 40) {
            progressBar.style.background = 'var(--primary-gradient)';
            progressMsg.textContent = "⚖️ Conditional. Negotiate for more.";
            document.getElementById('verdict-badge').textContent = "CONDITIONAL";
            document.getElementById('verdict-badge').className = "verdict-badge verdict-go"; // Keep green for positive vibes
        } else {
            progressBar.style.background = 'var(--danger-gradient)';
            progressMsg.textContent = "⚠️ Warning territory. Proceed with caution.";
            document.getElementById('verdict-badge').textContent = "WARNING";
            document.getElementById('verdict-badge').className = "verdict-badge verdict-nogo";
        }
    }

    function handlePreset(scenario) {
        state.simulations++;
        document.getElementById('sim-count').textContent = state.simulations;

        // Easter Egg / Gamification
        if (state.simulations === 5) {
            flashMessage("🔓 Expert Mode Unlocked!");
        }

        switch (scenario) {
            case 'roommate':
                flashMessage("🏠 Housing cost reduced by 40%!");
                // Simulate impact
                break;
            case 'married':
                flashMessage("💍 Tax filing status: Married Jointly");
                break;
            case 'max401k':
                slider401k.value = 20; // 2026 limit approx
                state.contribution401k = 20;
                val401k.textContent = '20%';
                flashMessage("📈 Retirement secured!");
                break;
            case 'optimize':
                animateSlider(salarySlider, state.salary + 15000);
                flashMessage("✨ AI suggests: Ask for $15k signing bonus");
                break;
            default:
                // reset
                break;
        }
        updateUI();
    }

    function updateRetirementImpact() {
        const imp = document.getElementById('retirement-imp');
        if (state.contribution401k > 15) imp.textContent = "FIRE 🔥";
        else if (state.contribution401k > 10) imp.textContent = "Aggressive 🚀";
        else if (state.contribution401k > 5) imp.textContent = "Healthy ✅";
        else imp.textContent = "Basic";
    }

    function animateValue(id, start, end, duration, suffix = '') {
        // Simple swap for now (can upgrade to CountUp.js)
        const el = document.getElementById(id);
        if (!el) return;
        el.textContent = end + suffix;
        el.classList.remove('animate-in');
        void el.offsetWidth; // trigger reflow
        el.classList.add('animate-in');
    }

    function animateSlider(element, targetValue) {
        // Smooth slide animation logic could go here
        element.value = targetValue;
        state.salary = targetValue;
        updateUI();
    }

    function flashMessage(msg) {
        const fomo = document.querySelector('.fomo-msg');
        if (!fomo) return;
        const original = fomo.textContent;
        fomo.textContent = msg;
        fomo.style.color = '#38a169'; // Green success
        setTimeout(() => {
            fomo.textContent = original;
            fomo.style.color = '#e53e3e'; // Back to warning
        }, 3000);
    }

    // --- SHARE FUNCTIONALITY ---
    document.getElementById('share-btn')?.addEventListener('click', function () {
        const receipt = document.querySelector('.receipt-card');
        const btn = this;
        const originalText = btn.innerText;

        btn.innerText = "Generating...";

        html2canvas(receipt, {
            scale: 2, // High resolution
            backgroundColor: '#ffffff',
            useCORS: true
        }).then(canvas => {
            const link = document.createElement('a');
            link.download = 'OfferVerdict-Receipt-' + new Date().toISOString().slice(0, 10) + '.png';
            link.href = canvas.toDataURL();
            link.click();

            btn.innerText = "✅ Downloaded!";
            setTimeout(() => btn.innerText = originalText, 2000);
        }).catch(err => {
            console.error("Capture failed:", err);
            btn.innerText = "❌ Error";
        });
    });
});
