document.body.addEventListener("showAlert", function (e) {
    alert(e.detail.text);
})

window.urlSearchParams = new URLSearchParams(location.search);

function setToSearchParams(searchParams, name, value) {
    // debugger;
    if (value === '' || value === undefined || value === null) {
        searchParams.delete(name);
    } else {
        searchParams.set(name, value);
    }
}