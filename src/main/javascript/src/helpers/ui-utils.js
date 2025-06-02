export function widenContainer(from = "col-md-6", to = "col-md-10") {
    const element = document.getElementsByClassName("steps-container-col")[0];
    element.classList.remove(from);
    element.classList.add(to);
}

export function shrinkContainer(from = "col-md-10", to = "col-md-6") {
    const element = document.getElementsByClassName("steps-container-col")[0];
    element.classList.remove(from);
    element.classList.add(to);
}

export function adjustBodyTopPadding(to = "pt-4", from = "pt-4") {
    const element = document.getElementsByClassName("experiment-steps__body")[0];
    element.classList.remove(from);
    element.classList.add(to);
}
