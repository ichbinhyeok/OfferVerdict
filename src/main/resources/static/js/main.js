// ============================================
// OFFERVERDICT V3.0 - Life Simulator Edition
// 완벽한 이벤트 바인딩 및 실시간 시나리오 조정
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
// GLOBAL STATE - window.appState
// ============================================

window.appState = {
    // Base values from server
    grossIncome: 0,
    netMonthly: 0,
    baseRent: 0,
    baseLivingCost: 0,
    baseTransport: 0,
    baseGroceries: 0,
    baseMisc: 0,
    taxes: 0,
    // Calculated values
    adjustedHousingCost: 0,
    adjustedTransport: 0,
    adjustedDining: 0,
    adjustedLivingCost: 0,
    residual: 0,
    deltaPercent: 0,
    // Reference values
    currentResidual: 0,
    // City data
    cityBAvgHousePrice: 0,
    // User adjustments from Life Simulator
    housingScenario: 'RENT', // 'RENT', 'OWN', 'PARENTS'
    hasRoommate: false,
    rentAdjustment: 0, // User-adjusted rent value
    transportMode: 'car', // 'car', 'transit'
    diningLevel: 50 // 0-100
};

let previousVerdict = null;
let confettiActive = false;

// ============================================
// VERDICT CLASSIFICATION LOGIC
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
// 통합 재계산 함수 - recalculateAll()
// ============================================

function recalculateAll() {
    console.log('[recalculateAll] 🚀 Starting unified recalculation...');
    console.log('[recalculateAll] Current state:', {
        housingScenario: window.appState.housingScenario,
        hasRoommate: window.appState.hasRoommate,
        rentAdjustment: window.appState.rentAdjustment,
        transportMode: window.appState.transportMode,
        diningLevel: window.appState.diningLevel
    });
    
    // A. 보정된 주거비 계산
    let housingCost = 0;
    
    switch (window.appState.housingScenario) {
        case 'RENT':
            // Use user-adjusted rent value (or base rent if not adjusted)
            housingCost = window.appState.rentAdjustment || window.appState.baseRent;
            if (window.appState.hasRoommate) {
                housingCost = housingCost * 0.5;
            }
            console.log('[recalculateAll] 💰 RENT mode:', {
                baseRent: window.appState.baseRent,
                adjustedRent: window.appState.rentAdjustment,
                hasRoommate: window.appState.hasRoommate,
                finalCost: housingCost
            });
            break;
        case 'OWN':
            // Property Tax + Maintenance = 1.5% of property value / 12
            housingCost = (window.appState.cityBAvgHousePrice * 0.015) / 12.0;
            console.log('[recalculateAll] 🏡 OWN mode:', {
                avgHousePrice: window.appState.cityBAvgHousePrice,
                propertyTaxMaintenance: housingCost
            });
            break;
        case 'PARENTS':
            // $300 social cost
            housingCost = 300;
            console.log('[recalculateAll] 👨‍👩‍👦 PARENTS mode: Social Cost = $300');
            break;
    }
    
    window.appState.adjustedHousingCost = housingCost;
    
    // B. Transport 계산 (추후 구현 가능)
    window.appState.adjustedTransport = window.appState.baseTransport;
    if (window.appState.transportMode === 'transit') {
        // 대중교통 사용 시 비용 절감 (예: -30%)
        window.appState.adjustedTransport = window.appState.baseTransport * 0.7;
        console.log('[recalculateAll] 🚇 Transit mode: 30% transport cost reduction');
    }
    
    // C. Dining 계산 (추후 구현 가능)
    // diningLevel: 0 = 모든 식사 집에서, 100 = 모든 식사 외식
    const diningMultiplier = 0.5 + (window.appState.diningLevel / 100) * 0.5; // 0.5x ~ 1.0x
    window.appState.adjustedDining = window.appState.baseGroceries * diningMultiplier;
    console.log('[recalculateAll] 🍽️ Dining level:', window.appState.diningLevel + '%, multiplier:', diningMultiplier);
    
    // D. Total Living Cost
    window.appState.adjustedLivingCost = 
        window.appState.adjustedTransport + 
        window.appState.adjustedDining + 
        window.appState.baseMisc;
    
    console.log('[recalculateAll] 💵 Living costs:', {
        transport: window.appState.adjustedTransport.toFixed(2),
        dining: window.appState.adjustedDining.toFixed(2),
        misc: window.appState.baseMisc.toFixed(2),
        total: window.appState.adjustedLivingCost.toFixed(2)
    });
    
    // E. 새로운 잔여 소득(Residual) 계산
    const newResidual = window.appState.netMonthly - window.appState.adjustedHousingCost - window.appState.adjustedLivingCost;
    window.appState.residual = newResidual;
    
    // F. Delta Percent 계산
    const currentResidualAbs = Math.abs(window.appState.currentResidual);
    let newDeltaPercent = 0;
    if (currentResidualAbs === 0) {
        newDeltaPercent = newResidual === 0 ? 0 : newResidual > 0 ? 100 : -100;
    } else {
        newDeltaPercent = ((newResidual - window.appState.currentResidual) / currentResidualAbs) * 100;
    }
    window.appState.deltaPercent = newDeltaPercent;
    
    console.log('[recalculateAll] 📊 Results:', {
        housingCost: housingCost.toFixed(2),
        livingCost: window.appState.adjustedLivingCost.toFixed(2),
        newResidual: newResidual.toFixed(2),
        deltaPercent: newDeltaPercent.toFixed(2)
    });
    
    // G. UI 업데이트
    updateHeroSection();
    updateWaterfallChart();
    updateAssetProjection();
    updateMobileStickyHeader(); // Mobile sticky header
    checkGamification();
    
    console.log('[recalculateAll] ✅ Recalculation complete!');
}

// ============================================
// UI 업데이트 함수들
// ============================================

function updateHeroSection() {
    console.log('[UI] Updating hero section (Receipt View)...');
    
    const verdictHero = document.getElementById('verdictHero');
    const verdictBadge = document.getElementById('verdictBadge');
    const verdictCopy = document.getElementById('verdictCopy');
    const heroResidualValue = document.getElementById('heroResidualValue');
    const deltaPercentValue = document.getElementById('deltaPercentValue');
    
    // Receipt View specific elements
    const heroHousingValue = document.getElementById('heroHousingValue');
    const heroLivingValue = document.getElementById('heroLivingValue');
    const compact3YearValue = document.getElementById('compact3YearValue');
    const dreamTextCompact = document.getElementById('dreamTextCompact');
    
    const newVerdict = classifyVerdict(window.appState.deltaPercent);
    const verdictText = getVerdictText(newVerdict);
    const verdictCopyText = generateVerdictCopy(newVerdict, window.appState.deltaPercent);
    
    // Verdict Badge 업데이트
    if (verdictBadge) {
        verdictBadge.textContent = verdictText;
        console.log('[UI] ✅ Verdict badge updated to:', verdictText);
    }
    
    // Hero 배경색 업데이트 (클래스 변경)
    const verdictClass = 'verdict-' + newVerdict.toLowerCase().replace('_', '-');
    
    if (verdictHero) {
        // 기존 verdict 클래스 제거
        const oldClasses = Array.from(verdictHero.classList).filter(c => c.startsWith('verdict-'));
        oldClasses.forEach(c => verdictHero.classList.remove(c));
        // 새로운 verdict 클래스 추가
        verdictHero.classList.add(verdictClass);
        console.log('[UI] ✅ Hero class updated to:', verdictClass);
    }
    
    // Mobile Sticky Header 배경색도 동일하게 업데이트
    const mobileSticky = document.getElementById('mobileSticky');
    if (mobileSticky) {
        // 기존 verdict 클래스 제거
        const oldClasses = Array.from(mobileSticky.classList).filter(c => c.startsWith('verdict-'));
        oldClasses.forEach(c => mobileSticky.classList.remove(c));
        // 새로운 verdict 클래스 추가
        mobileSticky.classList.add(verdictClass);
        console.log('[UI] ✅ Mobile sticky class updated to:', verdictClass);
    }
    
    // Verdict Copy 업데이트
    if (verdictCopy) {
        verdictCopy.textContent = verdictCopyText;
    }
    
    // Receipt: Housing 업데이트
    if (heroHousingValue) {
        heroHousingValue.textContent = '-' + formatNumber(window.appState.adjustedHousingCost, '$') + '/mo';
    }
    
    // Receipt: Living Costs 업데이트
    if (heroLivingValue) {
        heroLivingValue.textContent = '-' + formatNumber(window.appState.adjustedLivingCost, '$') + '/mo';
    }
    
    // Receipt: Residual 값 업데이트
    if (heroResidualValue) {
        const span = heroResidualValue.querySelector('.rolling-number');
        if (span) {
            animateRollingNumber(span, Math.round(window.appState.residual), '$');
        } else {
            heroResidualValue.innerHTML = formatNumber(window.appState.residual, '$') + '<span class="unit">/mo</span>';
        }
    }
    
    // Receipt: Delta Percent 업데이트
    if (deltaPercentValue) {
        const span = deltaPercentValue.querySelector('.rolling-number');
        if (span) {
            animateRollingNumber(span, window.appState.deltaPercent, '', true);
        }
    }
    
    // Compact 3-Year Projection
    if (compact3YearValue) {
        const threeYearTotal = window.appState.residual * 36;
        compact3YearValue.textContent = formatNumber(threeYearTotal, '$');
    }
    
    // Compact Dream Text
    if (dreamTextCompact && window.appState.residual > 0) {
        const monthsToTesla = Math.ceil(45000 / window.appState.residual);
        if (monthsToTesla <= 24) {
            dreamTextCompact.textContent = `🚗 Tesla Model 3 in ${monthsToTesla} months`;
        } else if (monthsToTesla <= 60) {
            dreamTextCompact.textContent = `🏠 House down payment in ${Math.ceil(50000 / window.appState.residual)} months`;
        } else {
            dreamTextCompact.textContent = `💰 Saving ${formatNumber(window.appState.residual * 12, '$')}/year`;
        }
    } else if (dreamTextCompact) {
        dreamTextCompact.textContent = '⚠️ Optimize to start saving';
    }
}

function updateWaterfallChart() {
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
    
    // Base values
    const gross = window.appState.grossIncome / 12; // Monthly gross
    const taxes = window.appState.taxes / 12; // Monthly taxes
    const housingCost = window.appState.adjustedHousingCost;
    const residual = window.appState.residual;
    
    // Calculate percentages (relative to gross)
    const grossPercent = 100;
    const taxesPercent = (taxes / gross) * 100;
    const housingPercent = (housingCost / gross) * 100;
    const residualPercent = Math.max(0, (residual / gross) * 100);
    
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
    
    console.log('[Waterfall] ✅ Chart updated');
}

function updateAssetProjection() {
    const currentCityProjection = document.getElementById('currentCityProjection');
    const newCityProjection = document.getElementById('newCityProjection');
    const currentCityProgress = document.getElementById('currentCityProgress');
    const newCityProgress = document.getElementById('newCityProgress');
    const dreamText = document.getElementById('dreamText');
    
    const currentProjection = window.appState.currentResidual * 36;
    const newProjection = window.appState.residual * 36;
    
    // Update numbers with rolling animation
    if (currentCityProjection) {
        currentCityProjection.classList.add('number-rolling');
        animateRollingNumber(currentCityProjection, Math.round(currentProjection), '$');
        currentCityProjection.setAttribute('data-value', currentProjection);
        setTimeout(() => currentCityProjection.classList.remove('number-rolling'), 600);
    }
    
    if (newCityProjection) {
        newCityProjection.classList.add('number-rolling');
        animateRollingNumber(newCityProjection, Math.round(newProjection), '$');
        newCityProjection.setAttribute('data-value', newProjection);
        setTimeout(() => newCityProjection.classList.remove('number-rolling'), 600);
    }
    
    // Animate progress bars
    const maxProjection = Math.max(Math.abs(currentProjection), Math.abs(newProjection));
    
    if (currentCityProgress && maxProjection > 0) {
        const currentPercent = Math.max(5, (Math.abs(currentProjection) / maxProjection) * 100);
    setTimeout(() => {
            currentCityProgress.style.width = currentPercent + '%';
    }, 100);
    }
    
    if (newCityProgress && maxProjection > 0) {
        const newPercent = Math.max(5, (Math.abs(newProjection) / maxProjection) * 100);
        setTimeout(() => {
            newCityProgress.style.width = newPercent + '%';
        }, 100);
    }
    
    // Generate dynamic "dream" text
    if (dreamText && newProjection > 0) {
        const monthsToTesla = Math.ceil(45000 / (newProjection / 36)); // Tesla Model 3 ~$45k
        const monthsToHouse = Math.ceil(50000 / (newProjection / 36)); // Down payment ~$50k
        
        let dreamMessage = '';
        if (newProjection < 0) {
            dreamMessage = '⚠️ Your savings are negative. Consider adjusting your lifestyle to break even.';
        } else if (monthsToTesla <= 24) {
            dreamMessage = `🚗 At this rate, you can buy a <strong>Tesla Model 3</strong> in <strong>${monthsToTesla} months</strong>.`;
        } else if (monthsToHouse <= 36) {
            dreamMessage = `🏠 At this rate, you can save a <strong>house down payment</strong> in <strong>${monthsToHouse} months</strong>.`;
} else {
            const yearlySavings = Math.round((newProjection / 36) * 12);
            dreamMessage = `💰 You're saving <strong>$${formatNumber(yearlySavings, '')}/year</strong>. Keep building wealth!`;
        }
        
        dreamText.innerHTML = `<p>${dreamMessage}</p>`;
    }
    
    console.log('[Asset] ✅ Projections and dream text updated');
}

// ============================================
// 초기화 로직
// ============================================

function loadInitialData(data) {
    console.log('[Init] 📥 Loading initial data with precision:', data);
    
    // window.appState에 데이터 주입
    window.appState.grossIncome = data.grossIncome || 0;
    window.appState.netMonthly = data.offerNetMonthly || 0;
    window.appState.baseRent = data.offerRent || 0;
    window.appState.baseLivingCost = data.offerLivingCost || 0;
    window.appState.baseTransport = data.offerTransport || 0;
    window.appState.baseGroceries = data.offerGroceries || 0;
    window.appState.baseMisc = data.offerMisc || 0;
    window.appState.taxes = data.taxes || 0;
    window.appState.residual = data.offerResidual || 0;
    window.appState.currentResidual = data.currentResidual || 0;
    window.appState.deltaPercent = data.deltaPercent || 0;
    window.appState.cityBAvgHousePrice = data.cityBAvgHousePrice || 0;
    window.appState.offerSalary = data.offerSalary || 0;
    window.appState.cityBName = data.cityBName || 'City B';
    
    // Tax Breakdown for precision tooltips
    window.appState.taxBreakdown = data.taxBreakdown || {
        federal: 0,
        state: 0,
        socialSecurity: 0,
        medicare: 0,
        additionalMedicare: 0
    };
    
    // 초기 adjustedHousingCost 설정
    window.appState.adjustedHousingCost = window.appState.baseRent;
    window.appState.rentAdjustment = window.appState.baseRent;
    
    console.log('[Init] ✅ appState initialized with precision tax data');
    previousVerdict = classifyVerdict(window.appState.deltaPercent);
}

// ============================================
// 이벤트 리스너 초기화
// ============================================

function initEditPanel() {
    const editBtn = document.getElementById('editInputsBtn');
    const editPanel = document.getElementById('editPanel');
    
    if (!editBtn) {
        console.error('[Edit Panel] ❌ Button #editInputsBtn not found');
        return;
    }
    
    if (!editPanel) {
        console.error('[Edit Panel] ❌ Panel #editPanel not found');
        return;
    }
    
    console.log('[Edit Panel] ✅ Elements found, binding events...');
    
    editBtn.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        
            const isOpen = editPanel.classList.contains('open');
        console.log('[Edit Panel] 🔄 Toggling, current state:', isOpen);
        
        if (isOpen) {
            editPanel.classList.remove('open');
            editBtn.innerHTML = '<span>✏️</span> Edit Inputs';
            editBtn.classList.remove('pulse');
            console.log('[Edit Panel] ✅ Panel closed');
        } else {
            editPanel.classList.add('open');
            editBtn.innerHTML = '<span>✏️</span> Close';
            editBtn.classList.remove('pulse');
            console.log('[Edit Panel] ✅ Panel opened');
            
            // Auto-scroll to panel with smooth animation
            setTimeout(() => {
                editPanel.scrollIntoView({ 
                    behavior: 'smooth', 
                    block: 'nearest',
                    inline: 'nearest'
                });
            }, 150); // Wait for accordion animation to start
        }
    });
    
    // Add pulse animation on first load to draw attention
    setTimeout(() => {
        editBtn.classList.add('pulse');
    }, 1000);
    
    // Remove pulse after first hover
    editBtn.addEventListener('mouseenter', () => {
        editBtn.classList.remove('pulse');
    }, { once: true });
    
    console.log('[Edit Panel] ✅ Event listener bound with auto-scroll');
}

function initLifeSimulator() {
    console.log('[Life Simulator] 🎮 Initializing all controls...');
    
    // A. Housing Scenario Radio Buttons
    const housingRent = document.getElementById('housingRent');
    const housingOwn = document.getElementById('housingOwn');
    const housingParents = document.getElementById('housingParents');
    const rentSliderGroup = document.getElementById('rentSliderGroup');
    
    console.log('[Life Simulator] Housing radios found:', {
        rent: !!housingRent,
        own: !!housingOwn,
        parents: !!housingParents
    });
    
    if (housingRent) {
        housingRent.addEventListener('change', () => {
            if (housingRent.checked) {
                console.log('[Life Simulator] 🏠 Housing changed to: RENT');
                window.appState.housingScenario = 'RENT';
                if (rentSliderGroup) rentSliderGroup.style.display = 'block';
                recalculateAll();
            }
        });
    }
    
    if (housingOwn) {
        housingOwn.addEventListener('change', () => {
            if (housingOwn.checked) {
                console.log('[Life Simulator] 🏠 Housing changed to: OWN');
                window.appState.housingScenario = 'OWN';
                if (rentSliderGroup) rentSliderGroup.style.display = 'none';
                recalculateAll();
            }
        });
    }
    
    if (housingParents) {
        housingParents.addEventListener('change', () => {
            if (housingParents.checked) {
                console.log('[Life Simulator] 🏠 Housing changed to: PARENTS');
                window.appState.housingScenario = 'PARENTS';
                if (rentSliderGroup) rentSliderGroup.style.display = 'none';
                recalculateAll();
            }
        });
    }
    
    // B. Roommate Toggle
    const roommateToggle = document.getElementById('roommateToggle');
    if (roommateToggle) {
        roommateToggle.addEventListener('change', () => {
            window.appState.hasRoommate = roommateToggle.checked;
            console.log('[Life Simulator] 💑 Roommate toggle:', window.appState.hasRoommate);
            recalculateAll();
        });
        console.log('[Life Simulator] ✅ Roommate toggle bound');
    }
    
    // C. Rent Slider with Dynamic Floating Label
    const rentSlider = document.getElementById('rentSlider');
    const rentValue = document.getElementById('rentValue');
    if (rentSlider && rentValue) {
        // Update slider gradient progress
        const updateSliderProgress = () => {
            const min = parseFloat(rentSlider.min);
            const max = parseFloat(rentSlider.max);
            const value = parseFloat(rentSlider.value);
            const progress = ((value - min) / (max - min)) * 100;
            rentSlider.style.setProperty('--slider-progress', progress + '%');
        };
        
        updateSliderProgress(); // Initial progress
        
        rentSlider.addEventListener('input', () => {
            const value = parseFloat(rentSlider.value);
            window.appState.rentAdjustment = value;
            rentValue.textContent = formatNumber(value, '$');
            
            // Update slider gradient
            updateSliderProgress();
            
            // Dynamic savings calculation
            const savings = window.appState.baseRent - value;
            if (savings > 0) {
                rentValue.textContent = formatNumber(value, '$') + ' 💚';
                rentValue.style.color = 'var(--green-600)';
            } else if (savings < 0) {
                rentValue.textContent = formatNumber(value, '$') + ' 📈';
                rentValue.style.color = 'var(--red-600)';
            } else {
                rentValue.style.color = 'var(--blue-600)';
            }
            
            console.log('[Life Simulator] 💰 Rent slider:', value, 'Savings:', savings);
            recalculateAll();
        });
        console.log('[Life Simulator] ✅ Rent slider bound with dynamic feedback');
    }
    
    // D. Transport Radio Buttons
    const transportCar = document.getElementById('transportCar');
    const transportTransit = document.getElementById('transportTransit');
    
    if (transportCar) {
        transportCar.addEventListener('change', () => {
            if (transportCar.checked) {
                window.appState.transportMode = 'car';
                console.log('[Life Simulator] 🚗 Transport: CAR');
                recalculateAll();
            }
        });
    }
    
    if (transportTransit) {
        transportTransit.addEventListener('change', () => {
            if (transportTransit.checked) {
                window.appState.transportMode = 'transit';
                console.log('[Life Simulator] 🚇 Transport: TRANSIT');
                recalculateAll();
            }
        });
    }
    console.log('[Life Simulator] ✅ Transport radios bound');
    
    // E. Dining Slider with Dynamic Feedback
    const diningSlider = document.getElementById('diningSlider');
    const diningValue = document.getElementById('diningValue');
    if (diningSlider && diningValue) {
        // Update slider gradient progress
        const updateDiningProgress = () => {
            const progress = parseFloat(diningSlider.value);
            diningSlider.style.setProperty('--slider-progress', progress + '%');
        };
        
        updateDiningProgress(); // Initial progress
        
        diningSlider.addEventListener('input', () => {
            const value = parseFloat(diningSlider.value);
            window.appState.diningLevel = value;
            
            // Update slider gradient
            updateDiningProgress();
            
            // Dynamic emoji feedback
            let emoji = '🥗';
            if (value < 25) emoji = '👨‍🍳';
            else if (value < 50) emoji = '🍱';
            else if (value < 75) emoji = '🍕';
            else emoji = '🍔';
            
            diningValue.textContent = value + '% ' + emoji;
            console.log('[Life Simulator] 🍽️ Dining slider:', value);
            recalculateAll();
        });
        console.log('[Life Simulator] ✅ Dining slider bound with dynamic feedback');
    }
    
    console.log('[Life Simulator] ✅ All controls initialized!');
}

function initWaterfallChart() {
    console.log('[Waterfall] Initializing chart with precision tooltips...');
    
    // Add hover tooltips to waterfall bars
    const waterfallItems = document.querySelectorAll('.waterfall-item');
    waterfallItems.forEach((item, index) => {
        const bar = item.querySelector('.waterfall-bar');
        if (!bar) return;
        
        // Create tooltip
        const tooltip = document.createElement('div');
        tooltip.className = 'waterfall-tooltip';
        bar.appendChild(tooltip);
        
        // Update tooltip content on hover
        item.addEventListener('mouseenter', () => {
            const dataValue = parseFloat(bar.getAttribute('data-value')) || 0;
            const label = item.querySelector('.waterfall-label')?.textContent || '';
            
            let tooltipText = label + ': ' + formatNumber(dataValue, '$');
            
            // PRECISION BREAKDOWN from TaxCalculatorService
            if (label.includes('Taxes') && window.appState.taxBreakdown) {
                const { federal, state, socialSecurity, medicare, additionalMedicare } = window.appState.taxBreakdown;
                const federalMonthly = federal / 12;
                const stateMonthly = state / 12;
                const ssMonthly = socialSecurity / 12;
                const medicareMonthly = (medicare + additionalMedicare) / 12;
                
                tooltipText = `Federal: ${formatNumber(federalMonthly, '$')}\n`;
                tooltipText += `State: ${formatNumber(stateMonthly, '$')}\n`;
                tooltipText += `Social Security: ${formatNumber(ssMonthly, '$')}\n`;
                tooltipText += `Medicare: ${formatNumber(medicareMonthly, '$')}`;
            }
            
            // Housing tooltip with rent details
            if (label.includes('Housing')) {
                const cityName = window.appState.cityBName || 'City';
                const savings = window.appState.baseRent - window.appState.adjustedHousingCost;
                tooltipText = `${cityName} Rent: ${formatNumber(window.appState.baseRent, '$')}\n`;
                if (savings > 0) {
                    tooltipText += `Savings: ${formatNumber(savings, '$')} 💚`;
                } else if (savings < 0) {
                    tooltipText += `Increase: ${formatNumber(Math.abs(savings), '$')} 📈`;
                }
            }
            
            tooltip.textContent = tooltipText;
        });
    });
    
    updateWaterfallChart();
}

function initRollingNumbers() {
    const elements = document.querySelectorAll('.rolling-number');
    console.log('[Rolling] Found', elements.length, 'elements - initializing with stagger');
    
    elements.forEach((el, index) => {
        const target = parseFloat(el.getAttribute('data-target'));
        if (!isNaN(target)) {
            const prefix = el.getAttribute('data-prefix') || '';
            const isPercent = el.classList.contains('delta-percent');
            
            // Stagger animation for visual appeal
            setTimeout(() => {
                el.classList.add('number-rolling');
                animateRollingNumber(el, target, prefix, isPercent);
                setTimeout(() => el.classList.remove('number-rolling'), 600);
            }, index * 100);
        }
    });
}

function initCitySwapper() {
    const citySwap = document.getElementById('city-swap');
    if (!citySwap) {
        console.log('[City Swap] Element not found');
        return;
    }
    
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
    console.log('[City Swap] ✅ Listener bound');
}

/**
 * Initialize Mobile Sticky Header
 * Shows verdict and residual when scrolling down
 */
function initMobileStickyHeader() {
    const mobileSticky = document.getElementById('mobileSticky');
    if (!mobileSticky) {
        console.log('[Mobile Sticky] Element not found');
        return;
    }
    
    let lastScrollY = window.scrollY;
    const threshold = 200; // Show after scrolling 200px
    
    const updateStickyHeader = () => {
        const currentScrollY = window.scrollY;
        
        // Show when scrolling down past threshold
        if (currentScrollY > threshold && currentScrollY > lastScrollY) {
            mobileSticky.classList.add('visible');
        } else if (currentScrollY < threshold) {
            mobileSticky.classList.remove('visible');
        }
        
        lastScrollY = currentScrollY;
    };
    
    // Throttle scroll event
    let ticking = false;
    window.addEventListener('scroll', () => {
        if (!ticking) {
            window.requestAnimationFrame(() => {
                updateStickyHeader();
                ticking = false;
            });
            ticking = true;
        }
    });
    
    console.log('[Mobile Sticky] ✅ Initialized');
}

/**
 * Update mobile sticky header when verdict changes
 */
function updateMobileStickyHeader() {
    const verdictMini = document.getElementById('verdictMini');
    const residualMini = document.getElementById('residualMini');
    
    if (verdictMini) {
        const verdict = classifyVerdict(window.appState.deltaPercent);
        verdictMini.textContent = getVerdictText(verdict);
        verdictMini.className = 'verdict-mini verdict-' + verdict.toLowerCase().replace('_', '-');
    }
    
    if (residualMini) {
        residualMini.textContent = formatNumber(window.appState.residual, '$');
    }
}

function initFormValidation() {
    const form = document.getElementById('mainForm');
    if (!form) {
        console.log('[Form] No form found (probably result page)');
        return;
    }
    
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
    console.log('[Form] ✅ Validation bound');
}

// ============================================
// 애니메이션 함수
// ============================================

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
// GAMIFICATION
// ============================================

function checkGamification() {
    const newVerdict = classifyVerdict(window.appState.deltaPercent);
    
    // Check if verdict improved from negative to positive
    if (previousVerdict && 
        (previousVerdict === 'NO_GO' || previousVerdict === 'WARNING') &&
        (newVerdict === 'GO' || newVerdict === 'CONDITIONAL')) {
        triggerCelebration();
        triggerSuccessPulse(); // Green pulse on sticky header
    }
    
    previousVerdict = newVerdict;
}

/**
 * Trigger success pulse animation on verdict improvement
 */
function triggerSuccessPulse() {
    const verdictHero = document.getElementById('verdictHero');
    const mobileSticky = document.getElementById('mobileSticky');
    
    if (verdictHero) {
        verdictHero.classList.add('pulse-success');
        setTimeout(() => verdictHero.classList.remove('pulse-success'), 2000);
    }
    
    if (mobileSticky) {
        mobileSticky.style.animation = 'successPulse 1s cubic-bezier(0.25, 0.8, 0.25, 1) 2';
        setTimeout(() => mobileSticky.style.animation = '', 2000);
    }
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
        padding: 2rem 3rem;
        border-radius: 24px;
        font-weight: 800;
        font-size: 2rem;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        z-index: 10001;
        animation: popIn 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);
        text-align: center;
        backdrop-filter: blur(12px);
    `;
    
    message.innerHTML = `
        <div style="font-size: 3rem; margin-bottom: 0.5rem;">🎉</div>
        <div>You found a way to make it work!</div>
        <div style="font-size: 1rem; font-weight: 600; margin-top: 0.5rem; opacity: 0.9;">Keep optimizing!</div>
    `;
    
    document.body.appendChild(message);
    
    // Pulse animation
    setTimeout(() => {
        message.style.transform = 'translate(-50%, -50%) scale(1.05)';
        setTimeout(() => {
            message.style.transform = 'translate(-50%, -50%) scale(1)';
        }, 150);
    }, 300);
    
    setTimeout(() => {
        message.style.animation = 'popOut 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55)';
        setTimeout(() => message.remove(), 400);
    }, 2500);
}

// ============================================
// FORM VALIDATION (for index.html)
// ============================================

let currentStep = 1;
const totalSteps = 3;

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
}

// ============================================
// 메인 초기화 - DOMContentLoaded 사용
// ============================================

document.addEventListener('DOMContentLoaded', function() {
    console.log('');
    console.log('═══════════════════════════════════════════');
    console.log('🚀 OFFERVERDICT V3.0 - LIFE SIMULATOR');
    console.log('═══════════════════════════════════════════');
    console.log('');
    
    // INITIAL_DATA 확인 (result.html에만 존재)
    if (!window.INITIAL_DATA) {
        console.log('[Init] ℹ️  INITIAL_DATA not found - this is index.html');
        console.log('[Init] ✅ Initializing form validation only...');
        initFormValidation();
        console.log('[Init] ✅ Index page initialization complete');
        return;
    }
    
    console.log('[Init] ✅ INITIAL_DATA found - this is result.html');
    console.log('[Init] 📦 INITIAL_DATA:', window.INITIAL_DATA);
    
    // 초기 데이터 로드
    loadInitialData(window.INITIAL_DATA);
    
    // result.html 전용 컴포넌트 초기화
    console.log('');
    console.log('───────────────────────────────────────────');
    console.log('🔧 BINDING EVENT LISTENERS');
    console.log('───────────────────────────────────────────');
    
    initEditPanel();
    initLifeSimulator(); // 🎮 Life Simulator 완전 초기화
    initWaterfallChart();
    initRollingNumbers();
    initCitySwapper();
    initMobileStickyHeader(); // 📱 Mobile sticky header
    
    // 초기 재계산 (서버 데이터로 한 번 실행)
    console.log('');
    console.log('───────────────────────────────────────────');
    console.log('🔄 RUNNING INITIAL CALCULATION');
    console.log('───────────────────────────────────────────');
    
    recalculateAll();
    
    // Initial entrance animations for lifestyle items
    setTimeout(() => {
        const lifestyleItems = document.querySelectorAll('.lifestyle-item');
        lifestyleItems.forEach((item, index) => {
            item.style.opacity = '0';
            item.style.transform = 'scale(0.8)';
            setTimeout(() => {
                item.style.transition = 'all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1)';
                item.style.opacity = '1';
                item.style.transform = 'scale(1)';
            }, index * 100);
        });
    }, 300);
    
    // Initial progress bar animations
    setTimeout(() => {
        const currentProgress = document.getElementById('currentCityProgress');
        const newProgress = document.getElementById('newCityProgress');
        
        if (currentProgress) {
            const width = currentProgress.style.width || '0%';
            currentProgress.style.width = '0%';
            setTimeout(() => {
                currentProgress.style.transition = 'width 1s cubic-bezier(0.25, 0.8, 0.25, 1)';
                currentProgress.style.width = width;
            }, 100);
        }
        
        if (newProgress) {
            const width = newProgress.style.width || '0%';
            newProgress.style.width = '0%';
            setTimeout(() => {
                newProgress.style.transition = 'width 1s cubic-bezier(0.25, 0.8, 0.25, 1)';
                newProgress.style.width = width;
            }, 200);
        }
    }, 500);
    
    console.log('');
    console.log('═══════════════════════════════════════════');
    console.log('✅ RESULT PAGE INITIALIZATION COMPLETE');
    console.log('✨ THE GLASS LAB IS READY');
    console.log('═══════════════════════════════════════════');
    console.log('');
});

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
