/**
 * OfferVerdict Dashboard "Truth Engine"
 * Handles the Simulator Logic (Layer 4) enabling users to challenge the Verdict.
 */

document.addEventListener('DOMContentLoaded', () => {
    const simulateBtn = document.getElementById('btn-recalc');
    if (simulateBtn) {
        simulateBtn.addEventListener('click', runSimulation);
    }
});

async function runSimulation() {
    const btn = document.getElementById('btn-recalc');
    const originalText = btn.textContent;
    btn.textContent = 'Calculating Truth...';
    btn.disabled = true;

    try {
        const form = document.getElementById('simulationForm');
        const formData = new FormData(form);
        const params = new URLSearchParams(formData);

        // Fetch new verdict from the Truth Engine API
        const response = await fetch(`/api/calculate?${params.toString()}`);
        if (!response.ok) throw new Error('Simulation failed');

        const result = await response.json();

        updateLayer1_Verdict(result);
        updateLayer2_Evidence(result);
        updateLayer3_Feeling(result); // The Feeling Layer needs dynamic update too

        // Show success feedback
        btn.textContent = 'Verdict Updated';
        setTimeout(() => {
            btn.textContent = originalText;
            btn.disabled = false;
        }, 1000);

    } catch (error) {
        console.error("Simulation error:", error);
        btn.textContent = 'Error - Try Again';
        btn.disabled = false;
    }
}

/**
 * Layer 1: Update the Hook
 */
function updateLayer1_Verdict(result) {
    const verdictSection = document.querySelector('.verdict-section');
    const verdictText = document.querySelector('.verdict-text');
    const verdictSub = document.querySelector('.verdict-sub');
    const impactValue = document.querySelector('.impact-value');

    // Remove old classes
    verdictSection.classList.remove('GO', 'NO_GO', 'CONDITIONAL', 'WARNING');
    verdictSection.classList.add(result.verdict);

    verdictText.textContent = result.verdictCopy;
    verdictSub.textContent = result.valueDiffMsg;
    impactValue.textContent = result.monthlyGainStr;

    // Add pop animation re-trigger
    verdictText.classList.remove('animate-pop');
    void verdictText.offsetWidth; // trigger reflow
    verdictText.classList.add('animate-pop');
}

/**
 * Layer 2: Update the Receipts (Evidence)
 */
function updateLayer2_Evidence(result) {
    // Helper to format currency
    const fmt = (num) => '$' + Math.round(num).toLocaleString();
    const fmtSigned = (num) => (num < 0 ? '-' : '+') + '$' + Math.abs(Math.round(num)).toLocaleString();

    // City A (Current)
    updateReceiptCard(0, result.current);
    // City B (Offer)
    updateReceiptCard(1, result.offer);

    function updateReceiptCard(index, breakdown) {
        const card = document.querySelectorAll('.receipt-card')[index];
        if (!card) return;

        const rows = card.querySelectorAll('.line-item .value');
        // 0: Salary (Static in this MVP sim? Or updated if we allow salary edit? 
        // For now user only changed taxes/housing, so salary might stay same unless we added salary input)
        // Let's assume Salary is static in Sim for now, or update if passed.

        // 1: Total Tax
        rows[1].textContent = '-' + fmt(breakdown.taxResult.totalTax);

        // 2: Housing
        rows[2].textContent = '-' + fmt(breakdown.rent * 12);

        // 3: Residual
        rows[3].textContent = fmt(breakdown.residual * 12);
    }
}

/**
 * Layer 3: Update the Reality (Tesla/House Index)
 */
function updateLayer3_Feeling(result) {
    // Tesla Index
    const teslaCard = document.querySelectorAll('.reality-card')[0];
    const teslaStrong = teslaCard.querySelectorAll('strong');
    teslaStrong[0].textContent = result.current.monthsToBuyTesla.toFixed(1);
    teslaStrong[1].textContent = result.offer.monthsToBuyTesla.toFixed(1);

    // House Index
    const houseCard = document.querySelectorAll('.reality-card')[1];
    const houseStrong = houseCard.querySelectorAll('strong');
    houseStrong[0].textContent = result.current.yearsToBuyHouse.toFixed(1) + ' Years';
    houseStrong[1].textContent = result.offer.yearsToBuyHouse.toFixed(1) + ' Years';
}
