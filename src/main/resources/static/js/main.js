// ============================================
// OFFERVERDICT V3.0 - Correction-First UX
// Robust Event Binding & Data Persistence
// ============================================

// ============================================
// UTILITY FUNCTIONS
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

let currentState = {
    // Base values from server
    grossIncome: 0,
    netMonthly: 0,
    rent: 0,
    livingCost: 0,
    taxes: 0,
    residual: 0,
    currentResidual: 0,
    deltaPercent: 0,
    // City data
    cityAAvgHousePrice: 0,
    cityBAvgHousePrice: 0,
    // User preferences from Index page
    housingType: 'RENT',
    householdType: 'SINGLE'
};

let previousVerdict = null;
let confettiActive = false;
let isInitialized = false;

// ============================================
// INITIALIZATION - Using window.load for Thymeleaf
// ============================================

function initializeApp() {
    if (isInitialized) {
        console.warn('App already initialized, skipping...');
        return;
    }
    
    console.log('Initializing OfferVerdict V3.0...');
    console.log('INITIAL_DATA available:', !!window.INITIAL_DATA);
    
    // Load initial data first (if available)
    if (window.INITIAL_DATA) {
        console.log('Loading initial data:', window.INITIAL_DATA);
        loadInitialData(window.INITIAL_DATA);
    } else {
        console.error('INITIAL_DATA not found! Make sure Thymeleaf rendered the data.');
    }
    
    // Initialize components in order
    initEditPanel();
    initQuickCorrectionToggles();
    initWaterfallChart();
    initRollingNumbers();
    initCitySwapper();
    initFormValidation();
    
    // Apply initial housing state from server
    applyInitialHousingState();
    
    isInitialized = true;
    console.log('App initialized successfully');
}

// Use window.load to ensure all Thymeleaf-rendered data is ready
window.addEventListener('load', function() {
    // Small delay to ensure all inline scripts are executed
    setTimeout(initializeApp, 50);
});

// ============================================
// EDIT PANEL ACCORDION - Robust Event Binding
// ============================================

function initEditPanel() {
    const editBtn = document.getElementById('editInputsBtn');
    const editPanel = document.getElementById('editPanel');
    
    if (!editBtn) {
        console.error('[Edit Panel] Button #editInputsBtn not found in DOM');
        return;
    }
    
    if (!editPanel) {
        console.error('[Edit Panel] Panel #editPanel not found in DOM');
        return;
    }
    
    console.log('[Edit Panel] Elements found, binding events...');
    
    // Use multiple event types for robustness
    const togglePanel = function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        const isOpen = editPanel.classList.contains('open');
        console.log('[Edit Panel] Toggling panel, current state:', isOpen);
        
        if (isOpen) {
            editPanel.classList.remove('open');
            editBtn.innerHTML = '<span>✏️</span> Edit Inputs';
        } else {
            editPanel.classList.add('open');
            editBtn.innerHTML = '<span>✏️</span> Close';
        }
        
        // Verify the class was added/removed
        const verifyOpen = editPanel.classList.contains('open');
        if (!isOpen && !verifyOpen) {
            console.error('[Edit Panel] Failed to add "open" class! Panel element:', editPanel);
        } else if (isOpen && verifyOpen) {
            console.error('[Edit Panel] Failed to remove "open" class! Panel element:', editPanel);
        } else {
            console.log('[Edit Panel] Successfully toggled, new state:', verifyOpen);
        }
    };
    
    // Bind multiple event types
    editBtn.addEventListener('click', togglePanel);
    editBtn.addEventListener('touchstart', togglePanel);
    
    // Fallback: Check if panel opens after click
    editBtn.addEventListener('click', function() {
        setTimeout(function() {
            const isOpen = editPanel.classList.contains('open');
            const maxHeight = window.getComputedStyle(editPanel).maxHeight;
            if (!isOpen && maxHeight === '0px') {
                console.error('[Edit Panel] Panel did not open after click! maxHeight:', maxHeight);
            }
        }, 100);
    });
}

// ============================================
// QUICK CORRECTION TOGGLES
// ============================================

function initQuickCorrectionToggles() {
    const ownHomeToggle = document.getElementById('ownHomeToggle');
    const parentsToggle = document.getElementById('parentsToggle');
    const splitRentToggle = document.getElementById('splitRentToggle');
    
    if (ownHomeToggle) {
        ownHomeToggle.addEventListener('change', () => {
            if (ownHomeToggle.checked && parentsToggle) {
                parentsToggle.checked = false;
            }
            recalculateAndUpdate();
        });
    }
    
    if (parentsToggle) {
        parentsToggle.addEventListener('change', () => {
            if (parentsToggle.checked && ownHomeToggle) {
                ownHomeToggle.checked = false;
            }
            recalculateAndUpdate();
        });
    }
    
    if (splitRentToggle) {
        splitRentToggle.addEventListener('change', () => {
            recalculateAndUpdate();
        });
    }
}

// ============================================
// APPLY INITIAL HOUSING STATE FROM SERVER
// ============================================

function applyInitialHousingState() {
    const ownHomeToggle = document.getElementById('ownHomeToggle');
    const parentsToggle = document.getElementById('parentsToggle');
    
    console.log('[Initial State] Applying housing type:', currentState.housingType);
    
    // Set toggles based on server-side housing type
    if (currentState.housingType === 'OWN' && ownHomeToggle) {
        ownHomeToggle.checked = true;
        console.log('[Initial State] Set "Own Home" toggle to checked');
    } else if (currentState.housingType === 'PARENTS' && parentsToggle) {
        parentsToggle.checked = true;
        console.log('[Initial State] Set "Living with Parents" toggle to checked');
    }
    
    // Recalculate immediately to reflect initial state
    if (currentState.housingType === 'OWN' || currentState.housingType === 'PARENTS') {
        console.log('[Initial State] Recalculating with initial housing state...');
        recalculateAndUpdate();
    }
}

// ============================================
// DATA LOADING & RECALCULATION
// ============================================

function loadInitialData(data) {
    console.log('Loading initial data:', data);
    
    currentState = {
        grossIncome: data.grossIncome || 0,
        netMonthly: data.offerNetMonthly || 0,
        rent: data.offerRent || 0,
        livingCost: data.offerLivingCost || 0,
        taxes: data.taxes || 0,
        residual: data.offerResidual || 0,
        currentResidual: data.currentResidual || 0,
        deltaPercent: data.deltaPercent || 0,
        cityAAvgHousePrice: data.cityAAvgHousePrice || 0,
        cityBAvgHousePrice: data.cityBAvgHousePrice || 0,
        housingType: data.housingType || 'RENT',
        householdType: data.householdType || 'SINGLE'
    };
    
    console.log('State initialized:', currentState);
    previousVerdict = classifyVerdict(currentState.deltaPercent);
}

function recalculateAndUpdate() {
    console.log('[Recalculate] Starting recalculation...');
    
    const ownHomeToggle = document.getElementById('ownHomeToggle');
    const parentsToggle = document.getElementById('parentsToggle');
    const splitRentToggle = document.getElementById('splitRentToggle');
    
    // Calculate adjusted rent based on toggles
    let adjustedRent = currentState.rent;
    let housingCost = 0;
    
    if (parentsToggle && parentsToggle.checked) {
        // PARENTS mode: Rent = 0, Social Cost = $300
        adjustedRent = 0;
        housingCost = 300;
        console.log('[Recalculate] PARENTS mode: Rent=0, Social Cost=$300');
    } else if (ownHomeToggle && ownHomeToggle.checked) {
        // OWN mode: Rent = 0, Property Tax/Maintenance = 1.5% of property value / 12
        adjustedRent = 0;
        housingCost = (currentState.cityBAvgHousePrice * 0.015) / 12.0;
        console.log('[Recalculate] OWN mode: Rent=0, Property Tax/Maintenance=$' + housingCost.toFixed(2));
    }
    
    // Apply split rent (50% reduction)
    if (splitRentToggle && splitRentToggle.checked) {
        adjustedRent = adjustedRent * 0.5;
        console.log('[Recalculate] Split Rent: 50% reduction applied');
    }
    
    // Calculate new residual
    const totalHousingCost = adjustedRent + housingCost;
    const newResidual = currentState.netMonthly - totalHousingCost - currentState.livingCost;
    
    // Calculate new delta percent
    const currentResidualAbs = Math.abs(currentState.currentResidual);
    let newDeltaPercent = 0;
    if (currentResidualAbs === 0) {
        newDeltaPercent = newResidual === 0 ? 0 : newResidual > 0 ? 100 : -100;
    } else {
        newDeltaPercent = ((newResidual - currentState.currentResidual) / currentResidualAbs) * 100;
    }
    
    // Update global state
    currentState.residual = newResidual;
    currentState.deltaPercent = newDeltaPercent;
    
    console.log('[Recalculate] Results:', {
        adjustedRent,
        housingCost,
        totalHousingCost,
        newResidual,
        newDeltaPercent
    });
    
    // Chain reaction: Update all UI components
    updateVerdictDisplay();
    updateResidualDisplay();
    updateWaterfallChart(adjustedRent + housingCost, newResidual);
    updateAssetProjection();
    checkGamification();
}

// ============================================
// UI UPDATES - Complete Chain Reaction
// ============================================

function updateVerdictDisplay() {
    console.log('[UI Update] Updating verdict display...');
    
    const verdictHero = document.getElementById('verdictHero');
    const verdictBadge = document.getElementById('verdictBadge');
    const verdictCopy = document.getElementById('verdictCopy');
    
    const newVerdict = classifyVerdict(currentState.deltaPercent);
    const verdictText = getVerdictText(newVerdict);
    const verdictCopyText = generateVerdictCopy(newVerdict, currentState.deltaPercent);
    
    // Update badge
    if (verdictBadge) {
        verdictBadge.textContent = verdictText;
        console.log('[UI Update] Verdict badge updated to:', verdictText);
    } else {
        console.error('[UI Update] Verdict badge element not found!');
    }
    
    // Update hero class (this changes the background color)
    if (verdictHero) {
        // Remove old verdict class
        const oldClasses = Array.from(verdictHero.classList).filter(c => c.startsWith('verdict-'));
        oldClasses.forEach(c => verdictHero.classList.remove(c));
        // Add new verdict class
        const newClass = 'verdict-' + newVerdict.toLowerCase().replace('_', '-');
        verdictHero.classList.add(newClass);
        console.log('[UI Update] Verdict hero class updated to:', newClass);
    } else {
        console.error('[UI Update] Verdict hero element not found!');
    }
    
    // Update copy
    if (verdictCopy) {
        verdictCopy.textContent = verdictCopyText;
        console.log('[UI Update] Verdict copy updated');
    } else {
        console.error('[UI Update] Verdict copy element not found!');
    }
}

function updateResidualDisplay() {
    const heroResidualValue = document.getElementById('heroResidualValue');
    const deltaPercentValue = document.getElementById('deltaPercentValue');
    
    if (heroResidualValue) {
        const span = heroResidualValue.querySelector('.rolling-number');
        if (span) {
            animateRollingNumber(span, Math.round(currentState.residual), '$');
        } else {
            heroResidualValue.textContent = formatNumber(currentState.residual, '$');
        }
    }
    
    if (deltaPercentValue) {
        animateRollingNumber(deltaPercentValue, currentState.deltaPercent, '', true);
    }
}

function updateAssetProjection() {
    const currentCityProjection = document.getElementById('currentCityProjection');
    const newCityProjection = document.getElementById('newCityProjection');
    
    const currentProjection = currentState.currentResidual * 36;
    const newProjection = currentState.residual * 36;
    
    if (currentCityProjection) {
        currentCityProjection.textContent = formatNumber(currentProjection, '$');
        currentCityProjection.setAttribute('data-value', currentProjection);
    }
    
    if (newCityProjection) {
        newCityProjection.textContent = formatNumber(newProjection, '$');
        newCityProjection.setAttribute('data-value', newProjection);
    }
}

// ============================================
// WATERFALL CHART
// ============================================

function initWaterfallChart() {
    // Initial render
    updateWaterfallChart(currentState.rent, currentState.residual);
}

function updateWaterfallChart(housingCost, residual) {
    const grossBar = document.getElementById('waterfallGross');
    const taxesBar = document.getElementById('waterfallTaxes');
    const housingBar = document.getElementById('waterfallHousing');
    const residualBar = document.getElementById('waterfallResidual');
    
    const grossValue = document.getElementById('waterfallGrossValue');
    const taxesValue = document.getElementById('waterfallTaxesValue');
    const housingValue = document.getElementById('waterfallHousingValue');
    const residualValue = document.getElementById('waterfallResidualValue');
    
    if (!grossBar || !taxesBar || !housingBar || !residualBar) {
        console.error('[Waterfall] Chart elements not found!');
        return;
    }
    
    // Get base values
    const gross = currentState.grossIncome / 12; // Monthly gross
    const taxes = currentState.taxes / 12; // Monthly taxes
    const net = currentState.netMonthly;
    
    // Calculate percentages (relative to gross)
    const grossPercent = 100;
    const taxesPercent = (taxes / gross) * 100;
    const housingPercent = (housingCost / gross) * 100;
    const residualPercent = (residual / gross) * 100;
    
    // Update bar heights with CSS transition
    grossBar.style.height = grossPercent + '%';
    taxesBar.style.height = taxesPercent + '%';
    housingBar.style.height = housingPercent + '%';
    residualBar.style.height = residualPercent + '%';
    
    // Update values
    if (grossValue) grossValue.textContent = formatNumber(gross, '$');
    if (taxesValue) taxesValue.textContent = formatNumber(taxes, '$');
    if (housingValue) housingValue.textContent = formatNumber(housingCost, '$');
    if (residualValue) residualValue.textContent = formatNumber(residual, '$');
    
    // Update data attributes
    grossBar.setAttribute('data-value', gross);
    taxesBar.setAttribute('data-value', taxes);
    housingBar.setAttribute('data-value', housingCost);
    residualBar.setAttribute('data-value', residual);
    
    console.log('[Waterfall] Chart updated with heights:', {
        taxes: taxesPercent.toFixed(1) + '%',
        housing: housingPercent.toFixed(1) + '%',
        residual: residualPercent.toFixed(1) + '%'
    });
}

// ============================================
// VERDICT CLASSIFICATION
// ============================================

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

function generateVerdictCopy(verdict, deltaPercent) {
    const magnitude = Math.abs(deltaPercent);
    switch (verdict) {
        case 'GO':
            return magnitude > 20
                ? "You unlock " + Math.round(deltaPercent) + "% more life. Take the win."
                : "You gain " + Math.round(deltaPercent) + "% more breathing room. Take it.";
        case 'CONDITIONAL':
            return "Slight edge at +" + Math.round(deltaPercent) + "%. Negotiate perks then go.";
        case 'WARNING':
            return "This move squeezes you by " + Math.abs(Math.round(deltaPercent)) + "%. Push back hard.";
        case 'NO_GO':
            return "This offer makes you " + Math.abs(Math.round(deltaPercent)) + "% poorer. Walk away.";
        default:
            return "Calculating...";
    }
}

// ============================================
// GAMIFICATION
// ============================================

function checkGamification() {
    const newVerdict = classifyVerdict(currentState.deltaPercent);
    
    // Check if verdict improved from negative to positive
    if (previousVerdict && 
        (previousVerdict === 'NO_GO' || previousVerdict === 'WARNING') &&
        (newVerdict === 'GO' || newVerdict === 'CONDITIONAL')) {
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
    const message = document.createElement('div');
    message.className = 'success-message';
    message.style.cssText = `
        position: fixed;
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
// ROLLING NUMBER ANIMATION
// ============================================

function initRollingNumbers() {
    const elements = document.querySelectorAll('.rolling-number');
    
    elements.forEach((el) => {
        const target = parseFloat(el.getAttribute('data-target'));
        if (!isNaN(target)) {
            const prefix = el.getAttribute('data-prefix') || '';
            const isPercent = el.classList.contains('delta-percent');
            animateRollingNumber(el, target, prefix, isPercent);
        }
    });
}

function animateRollingNumber(element, target, prefix = '', isPercent = false) {
    if (!element) return;
    
    const currentText = element.textContent.replace(/[^0-9.-]/g, '');
    const start = parseFloat(currentText) || 0;
    const duration = 1500;
    const startTime = Date.now();
    
    function animate() {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const easeOutCubic = 1 - Math.pow(1 - progress, 3);
        const current = start + (target - start) * easeOutCubic;
        
        if (isPercent) {
            element.textContent = formatPercent(current);
        } else if (prefix === '$') {
            element.textContent = formatNumber(Math.round(current), '$');
        } else {
            element.textContent = formatNumber(Math.round(current), prefix);
        }
        
        if (progress < 1) {
            requestAnimationFrame(animate);
        } else {
            // Final value
            if (isPercent) {
                element.textContent = formatPercent(target);
            } else if (prefix === '$') {
                element.textContent = formatNumber(Math.round(target), '$');
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
    if (!citySwap) return;
    
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
// FORM VALIDATION (for index.html)
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
