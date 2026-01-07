/**
 * OfferVerdict Dashboard "Simulation Lab 3.1"
 * Refined Authority: Simplified Receipts, Strategic Indicators.
 */

document.addEventListener('DOMContentLoaded', () => {
    const simulationForm = document.getElementById('simulationForm');
    const stickyBar = document.getElementById('stickyBar');
    if (!simulationForm) return;

    // 1. Sticky Bar Scroll Logic
    window.addEventListener('scroll', () => {
        if (window.scrollY > 400) {
            stickyBar?.classList.add('visible');
        } else {
            stickyBar?.classList.remove('visible');
        }
    });

    // 2. Debounced Update Logic
    const debouncedRunSimulation = debounce(runSimulation, 300);

    // 3. Lab Card Interaction
    const labCards = document.querySelectorAll('.lab-card');
    labCards.forEach(card => {
        card.addEventListener('click', () => {
            card.classList.toggle('active');
            const targetId = card.dataset.target;
            const input = document.getElementById(targetId);
            if (input) {
                input.value = card.classList.contains('active') ? 'true' : 'false';
            }
            runSimulation();
        });
    });

    // 4. Slider Interaction
    const sliders = simulationForm.querySelectorAll('input[type="range"]');
    sliders.forEach(slider => {
        slider.addEventListener('input', (e) => {
            const valSpan = document.getElementById('val-' + slider.name.toLowerCase());
            if (valSpan) {
                let displayVal = e.target.value;
                if (slider.name === 'equityMultiplier') {
                    displayVal = parseFloat(displayVal).toFixed(1) + 'x';
                } else if (slider.name === 'commuteTime') {
                    displayVal = displayVal + 'm';
                } else {
                    displayVal = '$' + parseInt(displayVal).toLocaleString();
                }
                valSpan.textContent = displayVal;
            }
            debouncedRunSimulation();
        });
    });
});

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

async function runSimulation() {
    const form = document.getElementById('simulationForm');
    const formData = new FormData(form);
    const params = new URLSearchParams(formData);

    // Pulse effect
    const pulseTargets = document.querySelectorAll('.verdict-display, .receipt-card, .reality-card, .sticky-verdict-bar');
    pulseTargets.forEach(el => el.classList.add('animate-pulse'));
    setTimeout(() => pulseTargets.forEach(el => el.classList.remove('animate-pulse')), 500);

    try {
        const response = await fetch(`/api/calculate?${params.toString()}`);
        if (!response.ok) throw new Error('Simulation failed');

        const result = await response.json();

        updateLayer1_Verdict(result);
        updateLayer2_Evidence(result);
        updateLayer3_Feeling(result);

    } catch (error) {
        console.error("Simulation error:", error);
    }
}

function updateLayer1_Verdict(result) {
    const verdictSection = document.querySelector('.verdict-section');
    const stickyBar = document.getElementById('stickyBar');
    const verdictTexts = document.querySelectorAll('.verdict-text, .sticky-text');
    const verdictSub = document.querySelector('.verdict-sub');
    const impactValues = document.querySelectorAll('.impact-value, .sticky-gain');
    const adviceText = document.querySelector('.leverage-msg span');
    const adviceGoal = document.querySelector('.leverage-msg strong');

    if (verdictSection) verdictSection.className = 'verdict-section ' + result.verdictColor;
    if (stickyBar) stickyBar.className = 'sticky-verdict-bar visible ' + result.verdictColor;

    verdictTexts.forEach(el => el.textContent = result.verdictCopy);
    if (verdictSub) verdictSub.textContent = result.valueDiffMsg;
    impactValues.forEach(el => el.textContent = result.monthlyGainStr);

    // Authority Advice
    if (adviceText) adviceText.textContent = result.authorityAdvice;
    if (adviceGoal) {
        if (result.reverseSalaryGoal > 0) {
            adviceGoal.style.display = 'inline';
            adviceGoal.textContent = '$' + Math.round(result.reverseSalaryGoal).toLocaleString();
        } else {
            adviceGoal.style.display = 'none';
        }
    }
}

function updateLayer2_Evidence(result) {
    const fmt = (num) => '$' + Math.round(num).toLocaleString();

    updateReceiptCard(0, result.current);
    updateReceiptCard(1, result.offer);

    function updateReceiptCard(index, breakdown) {
        const card = document.querySelectorAll('.receipt-card')[index];
        if (!card) return;

        const val_tax = card.querySelector('.line-tax');
        const val_local = card.querySelector('.line-local');
        const val_rent = card.querySelector('.line-rent');
        const val_res = card.querySelector('.line-residual');

        if (val_tax) val_tax.textContent = '-' + fmt(breakdown.taxResult.totalTax);
        if (val_local) val_local.textContent = '-' + fmt(breakdown.localTax * 12);
        if (val_rent) val_rent.textContent = '-' + fmt(breakdown.rent * 12);
        if (val_res) val_res.textContent = fmt(breakdown.residual * 12);
    }
}

function updateLayer3_Feeling(result) {
    const realityCards = document.querySelectorAll('.reality-card');
    const fmt = (num) => '$' + Math.round(num).toLocaleString();

    if (realityCards[0]) {
        const strongs = realityCards[0].querySelectorAll('strong');
        if (strongs.length >= 2) {
            strongs[0].textContent = result.current.monthsToBuyTesla.toFixed(1);
            strongs[1].textContent = result.offer.monthsToBuyTesla.toFixed(1);
        }
    }

    if (realityCards[1]) {
        const strongs = realityCards[1].querySelectorAll('strong');
        if (strongs.length >= 2) {
            strongs[0].textContent = result.current.yearsToBuyHouse.toFixed(1) + ' Years';
            strongs[1].textContent = result.offer.yearsToBuyHouse.toFixed(1) + ' Years';
        }
    }

    // Wealth Buffer Update
    const wealthVal = document.querySelector('.line-wealth');
    const investmentB = document.querySelector('.investment-row strong');
    if (wealthVal) wealthVal.textContent = result.wealthBufferMsg;
    if (investmentB) investmentB.textContent = fmt(result.investmentB);
}

// 5. Monetization & Traffic Tools
function shareOnX() {
    const text = encodeURIComponent(`My OfferVerdict for this move: ${document.querySelector('.verdict-text').textContent}. ${document.querySelector('.verdict-sub').textContent} #OfferVerdict #Career`);
    window.open(`https://twitter.com/intent/tweet?text=${text}&url=${encodeURIComponent(window.location.href)}`, '_blank');
}

function shareOnLinkedIn() {
    window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(window.location.href)}`, '_blank');
}

function copyLink() {
    navigator.clipboard.writeText(window.location.href).then(() => {
        alert("Link copied! Share the truth.");
    });
}
