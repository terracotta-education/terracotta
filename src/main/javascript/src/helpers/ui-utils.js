import store from "@/store/index";

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

export function getColor(property) {
    return document.documentElement.style.getPropertyValue(property);
}

export function deleteAttributesFromObservedElement(parentClass, nodeClass, elementClass, attributes) {
  // remove attributes from elements for
  const observerForAddedElement = new MutationObserver(function(mutationsList) {
    for (const mutation of mutationsList) {
      if (mutation.type === "childList" && mutation.addedNodes.length > 0) {
        for (const node of mutation.addedNodes) {
          if (node.nodeType === 1 && node.classList.contains(nodeClass)) {
            let elements = node.querySelectorAll(elementClass);
            elements = elements.length === 0 ? [node] : elements;
            elements.forEach((el) => {
              attributes.forEach((attribute) => {
                el.removeAttribute(attribute);
              });
            });
          }
        }
      }
    }
  });
  const parentElement = document.querySelectorAll(parentClass);
  if (parentElement.length > 0) {
    observerForAddedElement.observe(parentElement[0], { childList: true, subtree: true });
  } else {
    console.warn(`Parent element with class ${parentClass} not found. MutationObserver not set.`);
  }
}

export function addAttributesToObservedElement(parentClass, nodeClass, elementClass, attributes) {
  // add attributes to elements for
  const observerForAddedElement = new MutationObserver(function(mutationsList) {
    for (const mutation of mutationsList) {
      if (mutation.type === "childList" && mutation.addedNodes.length > 0) {
        for (const node of mutation.addedNodes) {
          if (node.nodeType === 1 && node.classList.contains(nodeClass)) {
            let elements = node.querySelectorAll(elementClass);
            elements = elements.length === 0 ? [node] : elements;
            elements.forEach((el) => {
              attributes.forEach((attribute) => {
                el.setAttribute(attribute.name, attribute.value);
              });
            });
          }
        }
      }
    }
  });
  const parentElement = document.querySelectorAll(parentClass);
  if (parentElement.length > 0) {
    observerForAddedElement.observe(parentElement[0], { childList: true, subtree: true });
  } else {
    console.warn(`Parent element with class ${parentClass} not found. MutationObserver not set.`);
  }
}

export function deleteAttributesFromElement(elementClass, attributes) {
  const elements = document.querySelectorAll(elementClass);
  elements.forEach((el) => {
    attributes.forEach((attribute) => {
      el.removeAttribute(attribute);
    });
  });
}

export function addAttributesToElement(elementClass, attributes) {
  const elements = document.querySelectorAll(elementClass);
  elements.forEach((el) => {
    attributes.forEach((attribute) => {
      el.setAttribute(attribute.name, attribute.value);
    });
  });
}

export function getAttributeFromElement(elementClass, attribute) {
  const element = document.querySelectorAll(elementClass);

  return element.length > 0 ? element[0].getAttribute(attribute) : null;
}

export function handleTooltipOpening(tooltipRef) {
  // close all other tooltips except the one passed in
  Object.keys(this.$refs)
    .filter(ref => ref !== tooltipRef)
    .forEach(ref => {
      if (this.$refs[ref] && this.$refs[ref][0]) {
        this.$refs[ref][0].close();
      }
    });
}

export function statusAlert(type, message) {
  return {
    alertType: type,
    alertMessage: message
  }
}

export function createStatusAlert(statusAlert) {
  store.dispatch(
    `alert/${statusAlert.alertType || store.getters["alert/statuses"].info}`,
    statusAlert.alertMessage
  );
}

export function showSkipLink(show) {
  store.dispatch("configuration/update", {
    name: "showSkipLink",
    value: show
  });
}
