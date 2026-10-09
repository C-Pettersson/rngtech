// RNGTech wiki templates: cycle tag ingredients, switch infobox variants, filter recipe tables.
(function () {
    function cycleSlots() {
        document.querySelectorAll(".rw-cycle").forEach(function (slot) {
            if (slot.matches(":hover")) {
                return;
            }
            var options = slot.querySelectorAll(":scope > .rw-alt");
            for (var i = 0; i < options.length; i++) {
                if (options[i].classList.contains("rw-on")) {
                    options[i].classList.remove("rw-on");
                    options[(i + 1) % options.length].classList.add("rw-on");
                    return;
                }
            }
        });
    }

    setInterval(cycleSlots, 2000);

    document.addEventListener("click", function (event) {
        var tab = event.target.closest(".rw-variant-tab");
        if (!tab) {
            return;
        }
        var box = tab.closest(".rw-infobox");
        box.querySelectorAll(".rw-variant-tab, .rw-variant-image").forEach(function (element) {
            element.classList.toggle("rw-on", element.dataset.index === tab.dataset.index);
        });
    });

    document.addEventListener("input", function (event) {
        if (!event.target.matches(".rw-filter")) {
            return;
        }
        var query = event.target.value.trim().toLowerCase();
        var table = document.getElementById(event.target.dataset.table);
        table.querySelectorAll("tbody tr").forEach(function (row) {
            row.hidden = query !== "" && row.dataset.search.indexOf(query) === -1;
        });
    });
})();
