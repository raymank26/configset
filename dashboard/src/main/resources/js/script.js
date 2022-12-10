import {initWidgets} from "./widgets.js";

document.body.addEventListener("showAlert", function (e) {
    alert(e.detail.text);
})

initWidgets();