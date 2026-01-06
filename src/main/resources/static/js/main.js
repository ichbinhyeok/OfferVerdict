// ============================================
// OFFERVERDICT V3.0 - Fully Working
// ============================================

// ============================================
// UTILITY FUNCTIONS - Number Formatting with Comma
// ============================================

function formatNumber(num, prefix = '') {
    if (isNaN(num) || num === null || num === undefined) {
        return prefix + '0';
    }
    const rounded = Math.round(parseFloat(num));
    const formatted = rounded.toLocaleString('en-US');
    return prefix + formatted;
}

function formatPercent(num) {
    if (isNaN(num) || num === null || num === undefined) {
        return '0%';
    }
    const value = parseFloat(num);
    return value.toFixed(1) + '%';
}

// ============================================
// GLOBAL STATE
// ============================================

let simulatorState = {
    baseRent: 0,
    baseNetMonthly: 0,
    baseLivingCost: 0,
    baseTransport: 0,
    baseGroceries: 0,
    baseMisc: 0,
    baseResidual: 0,
    currentResidual: 0,
    currentDeltaPercent: 0,
    baseDeltaPercent: 0
};

let previousVerdict = null;
let confettiActive = false;

// ============================================
// INITIALIZATION
// ============================================

// Wait for both DOM and inline scripts to load
function initializeApp() {
    console.log('Initializing app...');
    console.log('INITIAL_DATA available:', !!window.INITIAL_DATA);
    
    initEditPanel();
    
    // Load initial data first (if available)
    if (window.INITIAL_DATA) {
        console.log('Loading initial data:', window.INITIAL_DATA);
        loadInitialData(window.INITIAL_DATA);
    }
    
    // Then initialize simulator (wait a bit for DOM to be fully ready)
    setTimeout(() => {
        initLifeSimulator();
    }, 100);
    
    // Initialize rolling numbers (wait for elements to be ready)
    setTimeout(() => {
        initRollingNumbers();
    }, 200);
    
    // Initialize city swapper
    setTimeout(() => {
        initCitySwapper();
    }, 100);
    
    // Form validation for index page
    initFormValidation();
}

// Try multiple initialization strategies
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeApp);
} else {
    // DOM already loaded, but wait for inline scripts
    setTimeout(initializeApp, 100);
}

// ============================================
// EDIT PANEL ACCORDION
// ============================================

function initEditPanel() {
    const editBtn = document.getElementById('editInputsBtn');
    const editPanel = document.getElementById('editPanel');
    
    if (editBtn && editPanel) {
        editBtn.addEventListener('click', () => {
            editPanel.classList.toggle('open');
            const isOpen = editPanel.classList.contains('open');
            editBtn.innerHTML = isOpen ? '<span>✏️</span> Close' : '<span>✏️</span> Edit Inputs';
        });
    }
}

// ============================================
// LIFE SIMULATOR - FULLY WORKING
// ============================================

function initLifeSimulator() {
    console.log('Initializing Life Simulator...');
    
    const rentSlider = document.getElementById('rentSlider');
    const rentValue = document.getElementById('rentValue');
    const roommateToggle = document.getElementById('roommateToggle');
    const parentsToggle = document.getElementById('parentsToggle');
    const transportCar = document.getElementById('transportCar');
    const transportTransit = document.getElementById('transportTransit');
    const diningSlider = document.getElementById('diningSlider');
    const diningValue = document.getElementById('diningValue');
    
    if (!rentSlider) {
        console.log('Rent slider not found, skipping simulator init');
        console.log('Available elements:', {
            rentSlider: !!rentSlider,
            lifeSimulator: !!document.getElementById('lifeSimulator'),
            allInputs: document.querySelectorAll('input[type="range"]').length
        });
        return;
    }
    
    console.log('All simulator controls found');
    
    // Rent Slider - Real-time update with comma
    rentSlider.addEventListener('input', (e) => {
        const value = parseFloat(e.target.value);
        if (rentValue) {
            rentValue.textContent = formatNumber(value, '$');
        }
        updateSimulator();
    });
    
    // Roommate Toggle
    if (roommateToggle) {
        roommateToggle.addEventListener('change', () => {
            if (roommateToggle.checked && parentsToggle) {
                parentsToggle.checked = false;
            }
            updateSimulator();
        });
    }
    
    // Parents Toggle
    if (parentsToggle) {
        parentsToggle.addEventListener('change', () => {
            if (parentsToggle.checked && roommateToggle) {
                roommateToggle.checked = false;
            }
            updateSimulator();
        });
    }
    
    // Transport Radio
    if (transportCar && transportTransit) {
        transportCar.addEventListener('change', updateSimulator);
        transportTransit.addEventListener('change', updateSimulator);
    }
    
    // Dining Slider
    if (diningSlider && diningValue) {
        diningSlider.addEventListener('input', (e) => {
            diningValue.textContent = e.target.value + '%';
            updateSimulator();
        });
    }
    
    // Initial update
    updateSimulator();
}

function loadInitialData(data) {
    console.log('Loading initial data:', data);
    
    simulatorState = {
        baseRent: data.offerRent || 0,
        baseNetMonthly: data.offerNetMonthly || 0,
        baseLivingCost: data.offerLivingCost || 0,
        baseTransport: data.offerTransport || 0,
        baseGroceries: data.offerGroceries || 0,
        baseMisc: data.offerMisc || 0,
        baseResidual: data.offerResidual || 0,
        currentResidual: data.offerResidual || 0,
        currentDeltaPercent: data.deltaPercent || 0,
        baseDeltaPercent: data.deltaPercent || 0
    };
    
    console.log('Simulator state initialized:', simulatorState);
    
    // Set initial slider value
    const rentSlider = document.getElementById('rentSlider');
    if (rentSlider) {
        rentSlider.value = simulatorState.baseRent;
        const rentValue = document.getElementById('rentValue');
        if (rentValue) {
            rentValue.textContent = formatNumber(simulatorState.baseRent, '$');
        }
    }
    
    previousVerdict = classifyVerdict(simulatorState.currentDeltaPercent);
}

function updateSimulator() {
    const rentSlider = document.getElementById('rentSlider');
    const roommateToggle = document.getElementById('roommateToggle');
    const parentsToggle = document.getElementById('parentsToggle');
    const transportCar = document.getElementById('transportCar');
    const transportTransit = document.getElementById('transportTransit');
    const diningSlider = document.getElementById('diningSlider');
    
    if (!rentSlider) {
        console.log('Rent slider not found in updateSimulator');
        return;
    }
    
    // Calculate adjusted rent
    let adjustedRent = parseFloat(rentSlider.value);
    
    if (parentsToggle && parentsToggle.checked) {
        adjustedRent = 0;
    } else if (roommateToggle && roommateToggle.checked) {
        adjustedRent = adjustedRent * 0.6; // -40%
    }
    
    // Calculate transport adjustment
    let transportAdjustment = 0;
    if (transportTransit && transportTransit.checked) {
        // Save: gas + insurance (estimate 60% of transport)
        // Add: transit pass (estimate $100/month)
        transportAdjustment = (simulatorState.baseTransport * 0.6) - 100;
    }
    
    // Calculate dining adjustment
    let diningAdjustment = 0;
    if (diningSlider) {
        const diningPercent = parseFloat(diningSlider.value);
        // 0% = Home Cook (save 30% of groceries)
        // 100% = Foodie (spend 50% more on dining)
        const diningMultiplier = 1 + ((diningPercent / 100) * 0.8); // 1.0 to 1.8
        const baseDiningCost = simulatorState.baseGroceries + (simulatorState.baseMisc * 0.3);
        diningAdjustment = baseDiningCost * (1 - diningMultiplier);
    }
    
    // Social cost for living with parents
    const socialCost = (parentsToggle && parentsToggle.checked) ? 200 : 0;
    
    // Calculate new residual
    const adjustedLivingCost = simulatorState.baseLivingCost + transportAdjustment + diningAdjustment;
    const newResidual = simulatorState.baseNetMonthly - adjustedRent - adjustedLivingCost - socialCost;
    
    simulatorState.currentResidual = newResidual;
    
    // Calculate new delta percent
    const currentResidualAbs = Math.abs(simulatorState.baseResidual);
    if (currentResidualAbs === 0) {
        simulatorState.currentDeltaPercent = newResidual === 0 ? 0 : newResidual > 0 ? 100 : -100;
    } else {
        simulatorState.currentDeltaPercent = ((newResidual - simulatorState.baseResidual) / currentResidualAbs) * 100;
    }
    
    console.log('Updated residual:', newResidual, 'Delta:', simulatorState.currentDeltaPercent);
    
    // Update UI immediately with formatted numbers
    updateResidualDisplay(newResidual);
    updateVerdictDisplay();
    
    // Check for gamification trigger
    checkGamification();
}

function updateResidualDisplay(newResidual) {
    const heroResidualValue = document.getElementById('heroResidualValue');
    const newResidualValue = document.getElementById('newResidualValue');
    
    // Update hero residual (with rolling animation)
    if (heroResidualValue) {
        const span = heroResidualValue.querySelector('.rolling-number');
        if (span) {
            const target = Math.round(newResidual);
            animateRollingNumber(span, target, '$');
        } else {
            // If no rolling number element, update directly with comma
            heroResidualValue.textContent = formatNumber(newResidual, '$');
        }
    }
    
    // Update simulator result (with rolling animation)
    if (newResidualValue) {
        const target = Math.round(newResidual);
        animateRollingNumber(newResidualValue, target, '$');
    }
}

function updateVerdictDisplay() {
    const newVerdictBadge = document.getElementById('newVerdictBadge');
    const verdictHero = document.querySelector('.verdict-hero');
    const verdictBadge = verdictHero?.querySelector('.verdict-badge');
    
    const newVerdict = classifyVerdict(simulatorState.currentDeltaPercent);
    const verdictText = getVerdictText(newVerdict);
    
    // Update simulator result badge
    if (newVerdictBadge) {
        newVerdictBadge.textContent = verdictText;
        newVerdictBadge.className = 'preview-verdict verdict-' + newVerdict.toLowerCase().replace('_', '-');
    }
    
    // Update main hero verdict if changed significantly
    if (verdictHero && verdictBadge) {
        const oldVerdictClass = Array.from(verdictHero.classList).find(c => c.startsWith('verdict-'));
        if (oldVerdictClass) {
            verdictHero.classList.remove(oldVerdictClass);
        }
        verdictHero.classList.add('verdict-' + newVerdict.toLowerCase().replace('_', '-'));
        verdictBadge.textContent = verdictText;
    }
}

function classifyVerdict(deltaPercent) {
    if (deltaPercent >= 10) return 'GO';
    if (deltaPercent >= 0) return 'CONDITIONAL';
    if (deltaPercent > -10) return 'WARNING';
    return 'NO_GO';
}

function getVerdictText(verdict) {
    const map = {
        'GO': 'GO',
        'CONDITIONAL': 'CONDITIONAL',
        'WARNING': 'WARNING',
        'NO_GO': 'NO-GO'
    };
    return map[verdict] || 'UNKNOWN';
}

function checkGamification() {
    const newVerdict = classifyVerdict(simulatorState.currentDeltaPercent);
    
    // Check if verdict improved from negative to positive
    if (previousVerdict && 
        (previousVerdict === 'NO_GO' || previousVerdict === 'WARNING') &&
        (newVerdict === 'GO' || newVerdict === 'CONDITIONAL')) {
        
        // Trigger celebration
        triggerCelebration();
    }
    
    previousVerdict = newVerdict;
}

function triggerCelebration() {
    if (confettiActive) return;
    
    confettiActive = true;
    showConfetti();
    showSuccessMessage();
    
    setTimeout(() => {
        confettiActive = false;
    }, 3000);
}

function showConfetti() {
    const container = document.getElementById('confettiContainer');
    if (!container) return;
    
    const colors = ['#3b82f6', '#22c55e', '#eab308', '#f97316', '#ef4444'];
    const confettiCount = 100;
    
    for (let i = 0; i < confettiCount; i++) {
        const confetti = document.createElement('div');
        confetti.style.position = 'absolute';
        confetti.style.width = '10px';
        confetti.style.height = '10px';
        confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
        confetti.style.left = Math.random() * 100 + '%';
        confetti.style.top = '-10px';
        confetti.style.borderRadius = '50%';
        confetti.style.pointerEvents = 'none';
        confetti.style.zIndex = '10000';
        
        container.appendChild(confetti);
        
        const animation = confetti.animate([
            { transform: 'translateY(0) rotate(0deg)', opacity: 1 },
            { transform: `translateY(${window.innerHeight + 100}px) rotate(720deg)`, opacity: 0 }
        ], {
            duration: 3000 + Math.random() * 2000,
            easing: 'cubic-bezier(0.5, 0, 0.5, 1)'
        });
        
        animation.onfinish = () => confetti.remove();
    }
}

function showSuccessMessage() {
    const simulatorResult = document.getElementById('simulatorResult');
    if (!simulatorResult) return;
    
    const message = document.createElement('div');
    message.className = 'success-message';
    message.style.cssText = `
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: linear-gradient(135deg, #22c55e, #16a34a);
        color: white;
        padding: 1rem 2rem;
        border-radius: 12px;
        font-weight: 700;
        font-size: 1.25rem;
        box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
        z-index: 10001;
        animation: popIn 0.3s ease-out;
    `;
    message.textContent = '🎉 You fixed it!';
    
    document.body.appendChild(message);
    
    setTimeout(() => {
        message.style.animation = 'popOut 0.3s ease-out';
        setTimeout(() => message.remove(), 300);
    }, 2000);
}

// ============================================
// ROLLING NUMBER ANIMATION (with comma formatting)
// ============================================

function initRollingNumbers() {
    const elements = document.querySelectorAll('.rolling-number');
    console.log('Found rolling number elements:', elements.length);
    console.log('Elements:', Array.from(elements).map(el => ({
        id: el.id,
        target: el.getAttribute('data-target'),
        prefix: el.getAttribute('data-prefix')
    })));
    
    elements.forEach((el, index) => {
        const target = parseFloat(el.getAttribute('data-target'));
        if (!isNaN(target)) {
            const prefix = el.getAttribute('data-prefix') || '';
            console.log(`Animating element ${index}:`, target, prefix);
            animateRollingNumber(el, target, prefix);
        } else {
            console.log(`Skipping element ${index}: invalid target`, el.getAttribute('data-target'));
        }
    });
}

function animateRollingNumber(element, target, prefix = '') {
    if (!element) return;
    
    // Get current value from element text
    const currentText = element.textContent.replace(/[^0-9.-]/g, '');
    const start = parseFloat(currentText) || 0;
    const duration = 1500;
    const startTime = Date.now();
    
    function animate() {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const easeOutCubic = 1 - Math.pow(1 - progress, 3);
        const current = start + (target - start) * easeOutCubic;
        const rounded = Math.round(current);
        
        // Format with comma - ALWAYS use formatNumber
        if (prefix === '$') {
            element.textContent = formatNumber(rounded, '$');
        } else if (prefix === '%') {
            element.textContent = formatPercent(current);
        } else {
            element.textContent = formatNumber(rounded, prefix);
        }
        
        if (progress < 1) {
            requestAnimationFrame(animate);
        } else {
            // Final value with proper formatting
            if (prefix === '$') {
                element.textContent = formatNumber(Math.round(target), '$');
            } else if (prefix === '%') {
                element.textContent = formatPercent(target);
            } else {
                element.textContent = formatNumber(Math.round(target), prefix);
            }
        }
    }
    
    requestAnimationFrame(animate);
}

// ============================================
// CITY SWAPPER
// ============================================

function initCitySwapper() {
    const citySwap = document.getElementById('city-swap');
    if (!citySwap) {
        console.log('City swapper not found');
        console.log('Available selects:', document.querySelectorAll('select').length);
        return;
    }
    
    console.log('City swapper found, attaching listener');
    citySwap.addEventListener('change', (e) => {
        const newCity = e.target.value;
        if (newCity) {
            const currentUrl = window.location.pathname;
            const urlParts = currentUrl.split('-vs-');
            if (urlParts.length === 2) {
                const newUrl = urlParts[0] + '-vs-' + newCity + window.location.search;
                window.location.href = newUrl;
            }
        }
    });
}

// ============================================
// STEP FORM NAVIGATION (for index.html)
// ============================================

let currentStep = 1;
const totalSteps = 4;

function nextStep(step) {
    if (step > currentStep) {
        if (!validateStep(currentStep)) {
            return;
        }
    }
    currentStep = step;
    updateStepIndicator();
    showStep(step);
    updateReview();
}

function prevStep(step) {
    currentStep = step;
    updateStepIndicator();
    showStep(step);
}

function validateStep(step) {
    switch(step) {
        case 1:
            const job = document.getElementById('jobSelect')?.value;
            const cityA = document.getElementById('cityAInput')?.value;
            const cityB = document.getElementById('cityBInput')?.value;
            if (!job || !cityA || !cityB) {
                showValidationError('Please fill in all fields: Job Title, Current City, and Offer City.');
                return false;
            }
            return true;
        case 2:
            const currentSalary = parseFloat(document.getElementById('currentSalaryInput')?.value);
            const offerSalary = parseFloat(document.getElementById('offerSalaryInput')?.value);
            if (!currentSalary || !offerSalary || isNaN(currentSalary) || isNaN(offerSalary)) {
                showValidationError('Please enter both salaries as numbers.');
                return false;
            }
            if (currentSalary < 1000 || currentSalary > 10000000 || offerSalary < 1000 || offerSalary > 10000000) {
                showValidationError('Salaries must be between $1,000 and $10,000,000.');
                return false;
            }
            return true;
        default:
            return true;
    }
}

function showValidationError(message) {
    const existingError = document.querySelector('.validation-error');
    if (existingError) {
        existingError.remove();
    }
    
    const errorDiv = document.createElement('div');
    errorDiv.className = 'validation-error';
    errorDiv.style.cssText = 'background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 12px; padding: 1rem; margin-top: 1.5rem; color: #dc2626; font-size: 0.875rem; animation: slideUp 0.3s ease;';
    errorDiv.textContent = message;
    
    const currentStepGroup = document.querySelector(`.step-group[data-step="${currentStep}"]`);
    if (currentStepGroup) {
        currentStepGroup.appendChild(errorDiv);
        errorDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

function showStep(step) {
    document.querySelectorAll('.step-group').forEach(group => {
        group.classList.remove('active');
        group.style.display = 'none';
    });
    const activeGroup = document.querySelector(`.step-group[data-step="${step}"]`);
    if (activeGroup) {
        activeGroup.style.display = 'block';
        activeGroup.classList.add('active');
        requestAnimationFrame(() => {
            activeGroup.style.opacity = '0';
            activeGroup.style.transform = 'translateY(20px)';
            requestAnimationFrame(() => {
                activeGroup.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
                activeGroup.style.opacity = '1';
                activeGroup.style.transform = 'translateY(0)';
            });
        });
    }
}

function updateStepIndicator() {
    document.querySelectorAll('.step-dot').forEach((stepEl, index) => {
        const stepNum = index + 1;
        stepEl.classList.remove('active', 'completed');
        if (stepNum < currentStep) {
            stepEl.classList.add('completed');
        } else if (stepNum === currentStep) {
            stepEl.classList.add('active');
        }
    });
}

function updateReview() {
    const reviewJob = document.getElementById('reviewJob');
    const reviewCurrent = document.getElementById('reviewCurrent');
    const reviewOffer = document.getElementById('reviewOffer');
    const reviewHousehold = document.getElementById('reviewHousehold');
    
    if (reviewJob) {
        const jobSelect = document.getElementById('jobSelect');
        const selectedOption = jobSelect?.options[jobSelect?.selectedIndex];
        reviewJob.textContent = selectedOption?.text || '-';
    }
    
    if (reviewCurrent) {
        const cityA = document.getElementById('cityAInput')?.value;
        const currentSalary = document.getElementById('currentSalaryInput')?.value;
        reviewCurrent.textContent = cityA && currentSalary ? `${cityA} - $${formatNumber(parseInt(currentSalary), '')}` : '-';
    }
    
    if (reviewOffer) {
        const cityB = document.getElementById('cityBInput')?.value;
        const offerSalary = document.getElementById('offerSalaryInput')?.value;
        reviewOffer.textContent = cityB && offerSalary ? `${cityB} - $${formatNumber(parseInt(offerSalary), '')}` : '-';
    }
    
    if (reviewHousehold) {
        const household = document.getElementById('householdSelect')?.value;
        const housing = document.getElementById('housingSelect')?.value;
        reviewHousehold.textContent = household && housing ? `${household} / ${housing}` : '-';
    }
}

function initFormValidation() {
    const form = document.getElementById('mainForm');
    if (form) {
        form.addEventListener('submit', (e) => {
            let isValid = true;
            for (let step = 1; step <= 3; step++) {
                if (!validateStep(step)) {
                    isValid = false;
                    if (step !== currentStep) {
                        nextStep(step);
                    }
                    break;
                }
            }
            
            if (!isValid) {
                e.preventDefault();
                return false;
            }
            
            const job = document.getElementById('jobSelect')?.value;
            const cityA = document.getElementById('cityAInput')?.value;
            const cityB = document.getElementById('cityBInput')?.value;
            const currentSalary = document.getElementById('currentSalaryInput')?.value;
            const offerSalary = document.getElementById('offerSalaryInput')?.value;
            
            if (!job || !cityA || !cityB || !currentSalary || !offerSalary) {
                e.preventDefault();
                showValidationError('Please complete all steps before submitting.');
                return false;
            }
        });
    }
}

// ============================================
// CSS ANIMATIONS
// ============================================

const style = document.createElement('style');
style.textContent = `
    @keyframes popIn {
        from {
            opacity: 0;
            transform: translate(-50%, -50%) scale(0.8);
        }
        to {
            opacity: 1;
            transform: translate(-50%, -50%) scale(1);
        }
    }
    
    @keyframes popOut {
        from {
            opacity: 1;
            transform: translate(-50%, -50%) scale(1);
        }
        to {
            opacity: 0;
            transform: translate(-50%, -50%) scale(0.8);
        }
    }
    
    @keyframes slideUp {
        from {
            opacity: 0;
            transform: translateY(10px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
`;
document.head.appendChild(style);
