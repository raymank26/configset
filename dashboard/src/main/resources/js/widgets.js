export function initWidgets() {
    document.addEventListener('alpine:init', () => {
        Alpine.data('autocompleteForm', () => {
            return {
                'value': '',
                'autocompleteItems': [],
                'selectedSuggestionIndex': 0,
                init() {
                    this.autocompleteContainerId = this.$id('autocomplete');

                    let that = this;
                    this.$el.addEventListener('autocompleteItemsUpdated', () => {
                        let autocompleteContainer = document.getElementById(that.autocompleteContainerId);

                        that.selectedSuggestionIndex = -1;
                        that.autocompleteItems = autocompleteContainer.getElementsByTagName('div');
                    })
                    this.$el.addEventListener('focusout', () => {
                        that.autocompleteItems = [];
                        that.selectedSuggestionIndex = -1;
                    })
                    this.$refs.autocompleteInput.addEventListener('keydown', (e) => {
                        console.log(e.keyCode);
                        if (e.keyCode === 9) { // tab
                            return;
                        }
                        if (e.keyCode === 13) { // enter
                            if (that.selectedSuggestionIndex > 0) {
                                that.value = that.autocompleteItems[that.selectedSuggestionIndex].innerHTML;
                            }
                            that.autocompleteItems = []
                            that.selectedSuggestionIndex = -1;
                            e.preventDefault();
                            return;
                        } else if (e.keyCode === 38) { // up
                            that.selectedSuggestionIndex = Math.max(0, that.selectedSuggestionIndex - 1);
                        } else if (e.keyCode === 40 && that.autocompleteItems.length > 0) { // down
                            that.selectedSuggestionIndex = Math.min(that.autocompleteItems.length - 1,
                                that.selectedSuggestionIndex + 1);
                        } else {
                            that.$nextTick(() => {
                                e.target.dispatchEvent(new Event('autocompleteAjaxTrigger'));
                            })
                            return;
                        }
                        for (let item of that.autocompleteItems) {
                            item.classList.remove('autocomplete-active');
                        }
                        if (that.selectedSuggestionIndex >= 0) {
                            that.autocompleteItems[that.selectedSuggestionIndex].classList.add('autocomplete-active');
                        }
                    });
                    this.$refs.autocompleteResult.addEventListener('mousedown', (e) => {
                        that.value = e.target.innerHTML;
                        that.autocompleteItems = [];
                        that.selectedSuggestionIndex = -1;
                    })
                }
            }
        })
    });
}