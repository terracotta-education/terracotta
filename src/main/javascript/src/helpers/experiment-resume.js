const isBlank = value => !(typeof value === "string" && value.trim());

const isNotSet = value => !value || value === "NOSET";

/**
 * The page an experiment that isn't finished being set up should open on: the first design or
 * participation step whose answer hasn't been saved yet, so an instructor who used Save & Exit
 * comes back where they left off, with the steps before it shown as done. Needs the experiment
 * with its conditions. Returns null once design and participation are both done.
 */
export function experimentResumeRoute(experiment) {
  const conditions = experiment?.conditions || [];

  if (isBlank(experiment?.title)) {
    return "ExperimentDesignIntro";
  }

  if (isBlank(experiment.description)) {
    return "ExperimentDesignDescription";
  }

  if (!conditions.length || conditions.some(condition => isBlank(condition.name))) {
    return "ExperimentDesignConditions";
  }

  // a single condition experiment skips this step; saving its condition sets the type
  if (isNotSet(experiment.exposureType)) {
    return "ExperimentDesignType";
  }

  if (conditions.length > 1 && !conditions.some(condition => condition.defaultCondition)) {
    return "ExperimentDesignDefaultCondition";
  }

  if (isNotSet(experiment.participationType)) {
    return "ExperimentParticipationIntro";
  }

  if (experiment.participationType === "CONSENT") {
    if (isBlank(experiment.consent?.title)) {
      return "ParticipationTypeConsentTitle";
    }

    if (isBlank(experiment.consent?.fileName)) {
      return "ParticipationTypeConsentFile";
    }
  }

  if (isNotSet(experiment.distributionType)) {
    return experiment.participationType === "MANUAL"
      ? "ParticipationTypeManualSelection"
      : "ParticipationDistribution";
  }

  return null;
}
