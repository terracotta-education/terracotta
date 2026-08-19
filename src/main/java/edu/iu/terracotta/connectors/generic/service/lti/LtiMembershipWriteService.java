package edu.iu.terracotta.connectors.generic.service.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;

/**
 * Inserts an LtiMembershipEntity in its own transaction, independent of whatever ambient
 * transaction the caller is running in. lti_membership has a unique constraint on
 * (user_id, context_id), and a real LTI launch can race a roster sync to create the same
 * membership - if that insert fails inside the caller's own transaction, the failed, unflushable
 * pending entity poisons the caller's persistence context for the rest of that transaction
 * (Hibernate's "flushed after an exception" AssertionFailure on the next query). Isolating the
 * insert here means a lost race only fails this short-lived transaction, leaving the caller's own
 * session untouched so it can safely fall back to reading the winner's row.
 */
public interface LtiMembershipWriteService {

    LtiMembershipEntity insert(LtiMembershipEntity ltiMembershipEntity);

}
