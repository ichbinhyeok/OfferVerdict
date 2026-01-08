/**
 * Smart Autocomplete Module
 * Supports grouping (Categories, States) and custom rendering.
 */
class SmartAutocomplete {
    /**
     * @param {string} inputId - ID of the input element
     * @param {string} dropdownId - ID of the dropdown container
     * @param {object} data - Grouped data object { "Group Header": [ {title: "Item Name", ...}, ... ] }
     * @param {string} searchKey - Key in the item object to search/display (e.g., 'title' or 'city')
     * @param {string} valueKey - Key to set as input value (optional, defaults to searchKey)
     */
    constructor(inputId, dropdownId, data, searchKey = 'title', valueKey = null) {
        this.input = document.getElementById(inputId);
        this.dropdown = document.getElementById(dropdownId);
        this.data = data || {};
        this.searchKey = searchKey;
        this.valueKey = valueKey || searchKey;

        if (!this.input || !this.dropdown) return;

        this.init();
    }

    init() {
        // Debounce input slightly for performance
        let timeout;
        this.input.addEventListener('input', (e) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                const query = e.target.value.toLowerCase();
                this.render(query);
            }, 50);
        });

        this.input.addEventListener('focus', () => {
            this.render(this.input.value.toLowerCase());
        });

        // Close on click outside
        document.addEventListener('click', (e) => {
            if (!this.input.contains(e.target) && !this.dropdown.contains(e.target)) {
                this.dropdown.classList.remove('active');
            }
        });
    }

    render(query) {
        this.dropdown.innerHTML = '';
        let hasResults = false;

        for (const [group, items] of Object.entries(this.data)) {
            // Filter items in this group
            // Handles both 'title' (Job) and 'city' + 'state' (City) logic
            const matches = items.filter(item => {
                const text = this.getItemText(item).toLowerCase();
                return text.includes(query);
            });

            if (matches.length > 0) {
                hasResults = true;

                // Create Group Header
                const groupHeader = document.createElement('div');
                groupHeader.className = 'autocomplete-category';
                groupHeader.textContent = group;
                this.dropdown.appendChild(groupHeader);

                // Create Items
                matches.forEach(item => {
                    const el = document.createElement('div');
                    el.className = 'autocomplete-item';

                    const text = this.getItemText(item);

                    // Highlight match
                    const regex = new RegExp(`(${this.escapeRegExp(query)})`, 'gi');
                    const highlighted = text.replace(regex, '<span class="match">$1</span>');

                    el.innerHTML = highlighted;

                    el.addEventListener('click', () => {
                        this.input.value = text;
                        this.dropdown.classList.remove('active');
                    });

                    this.dropdown.appendChild(el);
                });
            }
        }

        if (hasResults) {
            this.dropdown.classList.add('active');
        } else {
            this.dropdown.classList.remove('active');
        }
    }

    getItemText(item) {
        if (this.searchKey === 'city_composite') {
            return `${item.city}, ${item.state}`;
        }
        return item[this.searchKey] || '';
    }

    escapeRegExp(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string
    }
}

// Initialize components when DOM is ready
document.addEventListener('DOMContentLoaded', function () {
    // 1. Job Autocomplete
    if (window.jobCategories) {
        new SmartAutocomplete('jobInput', 'jobDropdown', window.jobCategories, 'title');
    }

    // 2. City Autocomplete (Source & Destination)
    // We expect window.citiesByState to be injected
    if (window.citiesByState) {
        new SmartAutocomplete('cityInputA', 'cityDropdownA', window.citiesByState, 'city_composite');
        new SmartAutocomplete('cityInputB', 'cityDropdownB', window.citiesByState, 'city_composite');
    }
});
