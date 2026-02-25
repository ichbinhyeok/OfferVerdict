const fs = require('fs');
const path = require('path');

// Target High-Value Jobs
const targetJobs = [
    { slug: "software-engineer", name: "Software Engineer", prestige: "tech" },
    { slug: "data-scientist", name: "Data Scientist", prestige: "tech" },
    { slug: "product-manager", name: "Product Manager", prestige: "tech" },
    { slug: "registered-nurse", name: "Registered Nurse", prestige: "medical" },
    { slug: "financial-analyst", name: "Financial Analyst", prestige: "finance" },
    { slug: "marketing-manager", name: "Marketing Manager", prestige: "business" },
    { slug: "graphic-designer", name: "Graphic Designer", prestige: "creative" },
    { slug: "teacher", name: "Teacher", prestige: "education" },
    { slug: "police-officer", name: "Police Officer", prestige: "public-service" },
    { slug: "truck-driver", name: "Truck Driver", prestige: "logistics" },
    { slug: "civil-engineer", name: "Civil Engineer", prestige: "engineering" },
    { slug: "accountant", name: "Accountant", prestige: "finance" }
];

// Target High-Relocation Metros
const targetCities = [
    { slug: "new-york-ny", name: "New York City", taxState: "high", rentVibe: "extreme", region: "East Coast" },
    { slug: "san-francisco-ca", name: "San Francisco", taxState: "high", rentVibe: "extreme", region: "West Coast" },
    { slug: "seattle-wa", name: "Seattle", taxState: "zero", rentVibe: "expensive", region: "Pacific Northwest" },
    { slug: "austin-tx", name: "Austin", taxState: "zero", rentVibe: "expensive", region: "Sunbelt" },
    { slug: "dallas-tx", name: "Dallas", taxState: "zero", rentVibe: "moderate", region: "Sunbelt" },
    { slug: "houston-tx", name: "Houston", taxState: "zero", rentVibe: "moderate", region: "Sunbelt" },
    { slug: "chicago-il", name: "Chicago", taxState: "flat", rentVibe: "moderate", region: "Midwest" },
    { slug: "boston-ma", name: "Boston", taxState: "flat", rentVibe: "expensive", region: "East Coast" },
    { slug: "miami-fl", name: "Miami", taxState: "zero", rentVibe: "expensive", region: "Sunbelt" },
    { slug: "denver-co", name: "Denver", taxState: "flat", rentVibe: "expensive", region: "Rockies" },
    { slug: "atlanta-ga", name: "Atlanta", taxState: "flat", rentVibe: "expensive", region: "Southeast" },
    { slug: "los-angeles-ca", name: "Los Angeles", taxState: "high", rentVibe: "extreme", region: "West Coast" },
    { slug: "las-vegas-nv", name: "Las Vegas", taxState: "zero", rentVibe: "moderate", region: "Southwest" },
    { slug: "phoenix-az", name: "Phoenix", taxState: "flat", rentVibe: "expensive", region: "Southwest" },
    { slug: "orlando-fl", name: "Orlando", taxState: "zero", rentVibe: "moderate", region: "Sunbelt" },
    { slug: "nashville-tn", name: "Nashville", taxState: "zero", rentVibe: "expensive", region: "South" },
    { slug: "charlotte-nc", name: "Charlotte", taxState: "flat", rentVibe: "moderate", region: "Southeast" },
    { slug: "tampa-fl", name: "Tampa", taxState: "zero", rentVibe: "expensive", region: "Sunbelt" }
];

const salaryBuckets = [60000, 80000, 100000, 120000, 150000, 200000];

// Spintax / Logic Engine
function generateIntro(city, job, salary) {
    let text = "";
    let formatted = "$" + (salary / 1000).toFixed(0) + "k";

    // Tax logic
    let taxBenefit = "";
    if (city.taxState === "zero") {
        taxBenefit = `One massive advantage here is the lack of state income tax, which immediately boosts the liquidity of your ${formatted} base compared to working in California or New York. `;
    } else if (city.taxState === "high") {
        taxBenefit = `You must rigorously calculate your net take-home pay, as aggressive state and local tax brackets will severely clip the top-line power of this ${formatted} offer. `;
    } else {
        taxBenefit = `While not a zero-tax haven, the flat or moderate tax structures here mean your ${formatted} goes further than in the most brutal coastal hubs. `;
    }

    // Job logic
    if (job.prestige === "tech") {
        text = `Evaluating a ${formatted} compensation package for a ${job.name} in ${city.name} reveals a fascinating intersection of industry demand and local economic reality. ${taxBenefit}The tech ecosystem here is robust, but so is the competition for premium real estate.`;
    } else if (job.prestige === "medical") {
        text = `For a ${job.name} moving to ${city.name}, a ${formatted} salary establishes a solid foundation in the local healthcare system. ${taxBenefit}Given the grueling nature of clinical shifts, optimizing your commute and living costs is paramount to avoiding burnout.`;
    } else if (job.prestige === "finance") {
        text = `Commanding ${formatted} as a ${job.name} in ${city.name} signals excellent career positioning in a competitive sector. ${taxBenefit}Financial professionals often face high lifestyle inflation, so keeping fixed costs anchored will dictate your actual wealth accumulation.`;
    } else if (job.prestige === "education") {
        text = `Securing ${formatted} as a ${job.name} in ${city.name} is a vital benchmark. ${taxBenefit}Educators must balance proximity to quality districts with housing affordability, making this calculation critical.`;
    } else if (job.prestige === "public-service") {
        text = `Working as a ${job.name} in ${city.name} for ${formatted} involves a careful calculation of municipal benefits versus local living costs. ${taxBenefit}Balancing commute safety and housing is essential for public servants in this metro.`;
    } else if (job.prestige === "logistics") {
        text = `For a ${job.name}, a ${formatted} base in ${city.name} reflects the heavy demand for logistics and supply chain professionals. ${taxBenefit}Your mobility is a major asset depending on regional warehouse density.`;
    } else {
        text = `Securing a ${formatted} salary as a ${job.name} in ${city.name} places you squarely in the competitive professional class of the ${city.region}. ${taxBenefit}Your primary objective should be shielding these earnings from rapid cost-of-living increases.`;
    }
    return text;
}

function generateHousing(city, job, salary) {
    let warning = "";
    let formatted = "$" + (salary / 1000).toFixed(0) + "k";

    if (city.rentVibe === "extreme") {
        if (salary < 100000) warning = `WARNING: Housing in ${city.name} is famously unforgiving. Expect to spend well over 40% of your net income on rent, putting you dangerously close to being 'rent-burdened' unless you employ roommates or live deep in the outer boroughs.`;
        else warning = `While ${formatted} is a high salary nationally, in ${city.name}'s extreme housing market, you will likely hand over $2,500 to $3,500 for a decent 1-bedroom apartment. Proceed with caution to defend your savings rate.`;
    } else if (city.rentVibe === "expensive") {
        if (salary < 100000) warning = `Rent in ${city.name} will be your largest opponent. You'll need to allocate a strict 35% of your take-home pay to secure a modern apartment near decent transit or office hubs.`;
        else warning = `Housing affordability at this income bracket is entirely manageable. You can lease a premium unit in a desirable neighborhood while keeping your rent-to-net ratio around 25%.`;
    } else {
        warning = `The moderate housing costs in ${city.name} are where this ${formatted} salary truly shines. You can achieve an excellent standard of living, easily securing a spacious apartment while aggressively funneling surplus cash into investments.`;
    }
    return warning;
}

function generateAnalysis(city, job, salary) {
    if (salary <= 60000 && (city.rentVibe === "extreme" || city.rentVibe === "expensive")) {
        return `Verdict: NO-GO without drastic lifestyle adjustments. The math simply does not support wealth building for a ${job.name} at this tier in ${city.name}. Treat this solely as a stepping-stone resume builder.`;
    } else if (salary >= 150000) {
        return `Verdict: STRONG GO. The leverage provided by a ${"$" + (salary / 1000)}k salary creates an unshakable financial fortress. You have full command over housing choices and aggressive retirement maxing.`;
    } else {
        return `Verdict: CONDITIONAL. It is a strategically sound move for a ${job.name}, but inflation and lifestyle creep in ${city.name} are silent killers. Focus relentlessly on maintaining a 20% savings rate.`;
    }
}

// Generate the massive file
const results = [];
for (const city of targetCities) {
    for (const job of targetJobs) {
        for (const salary of salaryBuckets) {
            results.push({
                citySlug: city.slug,
                jobSlug: job.slug,
                salaryBucket: salary,
                introText: generateIntro(city, job, salary),
                housingWarning: generateHousing(city, job, salary),
                analysisText: generateAnalysis(city, job, salary)
            });
        }
    }
}

const outFile = path.join(__dirname, 'src', 'main', 'resources', 'data', 'AiPremiumReviews.json');
fs.writeFileSync(outFile, JSON.stringify(results, null, 2), 'utf-8');
console.log(`Generated ${results.length} highly context-rich SEO reviews to ${outFile}`);
