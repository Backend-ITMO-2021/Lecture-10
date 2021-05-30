var nickname = window.location.href.split("/").slice(-1)[0]
console.log(nickname)

function submitForm() {
    fetch("/", {
            method: "POST",
            body: JSON.stringify({to: toInput.value, name: nickname, msg: msgInput.value})
        }
    ).then(response => response.json())
        .then(json => {
            if (json["success"]) msgInput.value = ""
            errorDiv.innerText = json["err"]
        })
    return false;
}

function authForm() {
    console.log("Auth form handler")
    fetch("/auth", {
            method: "POST",
            body: JSON.stringify({userNickname:authNameInput.value})
        }
    ).then(response => response.json())
        .then(json => {
            console.log(json)
            if (json["success"]) window.location.href = `/users/${authNameInput.value}`;
            errorDiv.innerText = json["err"]
        })
    return false;
}

function changeDisplayForm() {
   console.log("Change display form handler")
   socket.send(`changeDisplay?`);
   return false;
}

function filterForm() {
    console.log("Filter form handler")
    const value = filterInput.value.trim();
    socket.send(`filter=${value}`);


    return false;
}

var socket = new WebSocket("ws://" + location.host + `/subscribe/${nickname}`);
socket.onmessage = function (ev) {
    let data = ev.data
    console.log(data)
    if (data.split("#")[0] == "display") {
        displayMode.innerHTML = data.split("#")[1] == "true" ? "<h2>Display: Cascade</h2>" : "<h2>Display: Column</h2>"
    } else if (data.split("#")[0] == "filter") {
        console.log("Received filter")
        filterInput.value = data.split("#")[1]
    } else {
        messageList.innerHTML = ev.data
    }
}