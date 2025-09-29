import moment from "moment";

export var validations = {
  hasErrors: false,
  container: {
    hasErrors: false,
    replyTo: {
      invalid: null,
      required: null
    },
    title: null,
    type: null,
    sendAt: {
      date: null,
      time: null
    }
  },
  message: {
    hasErrors: false,
    body: null,
    replyTo: {
      invalid: null,
      required: null
    },
    subject: null,
    recipients: {
      hasErrors: false,
      ruleSets: []
    },
    conditionalText: {
      hasErrors: false,
      label: null,
      result: null,
      ruleSets: []
    }
  }
}

export function initValidations() {
  return JSON.parse(JSON.stringify(validations));
}

function validateRuleSets(ruleSets, validationErrors) {
  validationErrors.ruleSets = [];

  if (ruleSets.length === 0) {
    return validationErrors;
  }

  ruleSets.forEach((ruleSet, index) => {
    validationErrors.ruleSets[index] = {
      message: null,
      hasRulesError: false,
      rules: []
    };

    if (!ruleSet.rules || !ruleSet.rules.length) {
      validationErrors.ruleSets[index].message = "Rule set must have at least one rule.";
      validationErrors.hasErrors = true;
    } else {
      ruleSet.rules.forEach((rule, ruleIndex) => {
        validationErrors.ruleSets[index].rules[ruleIndex] = {
          variable: null,
          comparison: null,
          value: null
        };

        if (!rule.assignment || !rule.assignment.lmsId) {
          // no rule variable selected
          validationErrors.ruleSets[index].rules[ruleIndex] = {
            ...validationErrors.ruleSets[index].rules[ruleIndex],
            variable: "Rule variable is required."
          };
          validationErrors.ruleSets[index].hasRulesError = true;
          validationErrors.hasErrors = true;
        }

        if (rule.assignment && (!rule.comparison || !rule.comparison.id)) {
          // has variable selected; no rule comparison selected
          validationErrors.ruleSets[index].rules[ruleIndex] = {
            ...validationErrors.ruleSets[index].rules[ruleIndex],
            comparison: "Rule comparison is required."
          };
          validationErrors.ruleSets[index].hasRulesError = true;
          validationErrors.hasErrors = true;
        }

        if (rule.assignment && rule.comparison && rule.comparison.requiresValue && (rule.value === null || rule.value === undefined || rule.value === "")) {
          // has variable and comparison selected; no rule value selected (if required)
          validationErrors.ruleSets[index].rules[ruleIndex] = {
            ...validationErrors.ruleSets[index].rules[ruleIndex],
            value: "Rule value is required."
          };
          validationErrors.ruleSets[index].hasRulesError = true;
          validationErrors.hasErrors = true;
        }
      });
    }
  });

  return validationErrors;
}

export function validateConditionalText(conditionalTexts, conditionalText) {
  let validationErrors = initValidations().message.conditionalText;

  validateLabel(conditionalTexts, conditionalText);
  validateResult(conditionalText);
  validationErrors = validateRuleSets(conditionalText.ruleSets, validationErrors);

  return validationErrors;

  function validateLabel(conditionalTexts, conditionalText) {
    if (!conditionalText.label || !conditionalText.label.trim()) {
      validationErrors.label = "Label is required.";
      validationErrors.hasErrors = true;
      return;
    }

    let exists = conditionalTexts
      .filter(ct => ct.id !== conditionalText.id)
      .some(ct => ct.label.trim() === conditionalText.label.trim());

    if (exists) {
      validationErrors.label = "Label already exists.";
      validationErrors.hasErrors = true;
    }
  }

  function validateResult(conditionalText) {
    if (!conditionalText.result.html/* || !conditionalText.result.json*/) {
      validationErrors.result = "Content to insert is required.";
      validationErrors.hasErrors = true;
    }
  }
}

export function validateContainer(container) {
  let validationErrors = initValidations().container;

  if (!container.configuration.title || !container.configuration.title.trim()) {
    validationErrors.title = "Title is required.";
    validationErrors.hasErrors = true;
  }

  if (!container.configuration.type || container.configuration.type === "NONE") {
    validationErrors.type = "Message type is required.";
    validationErrors.hasErrors = true;
  }

  if (!container.configuration.sendAt) {
    validationErrors.sendAt = {
      date: "Schedule date is required.",
      time: "Schedule time is required."
    };
    validationErrors.hasErrors = true;
  } else {
    if (!moment(container.configuration.sendAt).format("MM/DD/YYYY")) {
      validationErrors.sendAt.date = "Schedule date is required.";
    }
    if (!moment(container.configuration.sendAt, "HH:mm").format("h:mm A")) {
      validationErrors.sendAt.time = "Schedule time is required.";
    }
  }

  return validationErrors;
}

export function validateMessage(message, conditionalTexts, conditionalText) {
  let validationErrors = initValidations().message;

  if (!message.configuration.enabled) {
    // message is not enabled for sending; return validation
    return validationErrors;
  }

  validateSubject();
  validateBody();
  validationErrors = {
    ...validationErrors,
    recipients: validateRuleSets(message.ruleSets, validationErrors.recipients)
  };

  if (conditionalText) {
    validationErrors = {
      ...validationErrors,
      conditionalText: validateConditionalText(conditionalTexts, conditionalText)
    }
  }

  if (validationErrors.recipients.hasErrors || validationErrors.conditionalText.hasErrors) {
    validationErrors.hasErrors = true;
  }

  return validationErrors;

  function validateSubject() {
    if (!message.configuration.subject || !message.configuration.subject.trim()) {
      validationErrors.subject = "Subject line is required.";
      validationErrors.hasErrors = true;
    }
  }

  function validateBody() {
    if (!message.content.html || !message.content.html.trim()/* || !message.content.json || !message.content.json.trim()*/) {
      validationErrors.body = "Message body is required.";
      validationErrors.hasErrors = true;
    }
  }
}