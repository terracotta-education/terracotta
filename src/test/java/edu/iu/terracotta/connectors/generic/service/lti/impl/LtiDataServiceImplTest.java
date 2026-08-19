package edu.iu.terracotta.connectors.generic.service.lti.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiLinkEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiLinkRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.connectors.generic.service.lti.LtiMembershipWriteService;
import edu.iu.terracotta.dao.repository.LtiNonceRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.lti.Lti3Request;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class LtiDataServiceImplTest {

    private LtiContextRepository ltiContextRepository;
    private LtiLinkRepository ltiLinkRepository;
    private LtiMembershipRepository ltiMembershipRepository;
    private LtiMembershipWriteService ltiMembershipWriteService;
    private LtiUserRepository ltiUserRepository;
    private ToolDeploymentRepository toolDeploymentRepository;
    private PlatformDeploymentRepository platformDeploymentRepository;
    private LtiNonceRepository ltiNonceRepository;
    private EntityManager entityManager;

    private LtiDataServiceImpl ltiDataService;

    @BeforeEach
    public void beforeEach() {
        ltiContextRepository = mock(LtiContextRepository.class);
        ltiLinkRepository = mock(LtiLinkRepository.class);
        ltiMembershipRepository = mock(LtiMembershipRepository.class);
        ltiMembershipWriteService = mock(LtiMembershipWriteService.class);
        ltiUserRepository = mock(LtiUserRepository.class);
        toolDeploymentRepository = mock(ToolDeploymentRepository.class);
        platformDeploymentRepository = mock(PlatformDeploymentRepository.class);
        ltiNonceRepository = mock(LtiNonceRepository.class);
        entityManager = mock(EntityManager.class);

        ltiDataService = Mockito.spy(
            new LtiDataServiceImpl(
                ltiContextRepository,
                ltiLinkRepository,
                ltiMembershipRepository,
                ltiMembershipWriteService,
                ltiUserRepository,
                toolDeploymentRepository,
                platformDeploymentRepository,
                ltiNonceRepository
            )
        );

        // @PersistenceContext EntityManager is not constructor-injected, so @InjectMocks-style
        // wiring never populates it; the field must be set directly or every entityManager.xxx()
        // call in the service throws NullPointerException
        ReflectionTestUtils.setField(ltiDataService, "entityManager", entityManager);
    }

    // ---------------------------------------------------------------------
    // loadAndUpsertLTIDataInDB (pre-existing tests, kept as-is)
    // ---------------------------------------------------------------------

    @Test
    public void testLoadAndUpsertLTIDataInDBCallsBothInASingleTransactionalMethod() throws DataServiceException {
        Lti3Request lti = mock(Lti3Request.class);
        ToolDeployment toolDeployment = mock(ToolDeployment.class);

        doReturn(true).when(ltiDataService).loadLTIDataFromDB(eq(lti), eq("link"));
        doReturn(5).when(ltiDataService).upsertLTIDataInDB(eq(lti), eq(toolDeployment), eq("link"));

        int result = ltiDataService.loadAndUpsertLTIDataInDB(lti, toolDeployment, "link");

        assertEquals(5, result);

        // both calls must happen, in this order, so the entities loadLTIDataFromDB resolves
        // are still managed/attached within the same transaction when upsertLTIDataInDB runs
        InOrder inOrder = inOrder(ltiDataService);
        inOrder.verify(ltiDataService).loadLTIDataFromDB(lti, "link");
        inOrder.verify(ltiDataService).upsertLTIDataInDB(lti, toolDeployment, "link");
    }

    @Test
    public void testLoadAndUpsertLTIDataInDBReturnsUpsertResultRegardlessOfLoadResult() throws DataServiceException {
        Lti3Request lti = mock(Lti3Request.class);
        ToolDeployment toolDeployment = mock(ToolDeployment.class);

        // even when the load doesn't find existing data (new user/context), the upsert still
        // runs and its count is what's returned - loadLTIDataFromDB's boolean isn't the result
        doReturn(false).when(ltiDataService).loadLTIDataFromDB(any(Lti3Request.class), anyString());
        doReturn(2).when(ltiDataService).upsertLTIDataInDB(eq(lti), eq(toolDeployment), anyString());

        int result = ltiDataService.loadAndUpsertLTIDataInDB(lti, toolDeployment, "link");

        assertEquals(2, result);
    }

    // ---------------------------------------------------------------------
    // loadLTIDataFromDB
    // ---------------------------------------------------------------------

    @Test
    public void testLoadLTIDataFromDBReturnsFalseWhenDeploymentIdIsNull() {
        Lti3Request lti = mock(Lti3Request.class);
        doReturn(null).when(lti).getLtiDeploymentId();
        doReturn("aud1").when(lti).getAud();

        boolean result = ltiDataService.loadLTIDataFromDB(lti, "link1");

        assertFalse(result);
        verify(entityManager, never()).createQuery(anyString());
    }

    @Test
    public void testLoadLTIDataFromDBReturnsFalseWhenAudIsNull() {
        Lti3Request lti = mock(Lti3Request.class);
        doReturn("dep1").when(lti).getLtiDeploymentId();
        doReturn(null).when(lti).getAud();

        boolean result = ltiDataService.loadLTIDataFromDB(lti, "link1");

        assertFalse(result);
        verify(entityManager, never()).createQuery(anyString());
    }

    @Test
    public void testLoadLTIDataFromDBParsesLinkFromTargetLinkUrlWhenLinkIsNull() {
        Lti3Request lti = mock(Lti3Request.class);
        Query query = mock(Query.class);
        doReturn("dep1").when(lti).getLtiDeploymentId();
        doReturn("aud1").when(lti).getAud();
        doReturn("https://example.com/launch?link=9999").when(lti).getLtiTargetLinkUrl();
        doReturn(query).when(entityManager).createQuery(anyString());
        doReturn(Collections.emptyList()).when(query).getResultList();
        doReturn(false).when(lti).isLoaded();

        boolean result = ltiDataService.loadLTIDataFromDB(lti, null);

        assertFalse(result);
        verify(query).setParameter("link", "9999");
    }

    @Test
    public void testLoadLTIDataFromDBReturnsIsLoadedWhenNoRowsFound() {
        Lti3Request lti = mock(Lti3Request.class);
        Query query = mock(Query.class);
        doReturn("dep1").when(lti).getLtiDeploymentId();
        doReturn("aud1").when(lti).getAud();
        doReturn(query).when(entityManager).createQuery(anyString());
        doReturn(Collections.emptyList()).when(query).getResultList();
        doReturn(true).when(lti).isLoaded();

        // exercises the ltiDataVerboseLoggingEnabled branch's log line as well
        ReflectionTestUtils.setField(ltiDataService, "ltiDataVerboseLoggingEnabled", true);

        boolean result = ltiDataService.loadLTIDataFromDB(lti, "link1");

        assertTrue(result);
        verify(lti, never()).setLoaded(true);
    }

    @Test
    public void testLoadLTIDataFromDBPopulatesAllLtiFieldsWhenFullRowFound() {
        Lti3Request lti = mock(Lti3Request.class);
        Query query = mock(Query.class);
        PlatformDeployment platformDeployment = PlatformDeployment.builder().keyId(1L).iss("iss1").clientId("client1").build();
        ToolDeployment toolDeployment = ToolDeployment.builder().deploymentId(10L).ltiDeploymentId("dep1").platformDeployment(platformDeployment).build();
        LtiContextEntity context = new LtiContextEntity("ctx1", toolDeployment, "Context Title", "json");
        LtiLinkEntity link = new LtiLinkEntity("link1", context, "Link Title");
        LtiUserEntity user = new LtiUserEntity("user1", new Date(), platformDeployment);
        LtiMembershipEntity membership = new LtiMembershipEntity(context, user, 1);

        Object[] row = new Object[] {platformDeployment, context, link, membership, user, toolDeployment};

        doReturn("dep1").when(lti).getLtiDeploymentId();
        doReturn("aud1").when(lti).getAud();
        doReturn(query).when(entityManager).createQuery(anyString());
        doReturn(Collections.singletonList(row)).when(query).getResultList();
        doReturn(true).when(lti).isLoaded();

        boolean result = ltiDataService.loadLTIDataFromDB(lti, "link1");

        assertTrue(result);
        verify(lti).setKey(platformDeployment);
        verify(lti).setContext(context);
        verify(lti).setLink(link);
        verify(lti).setMembership(membership);
        verify(lti).setUser(user);
        verify(lti).setToolDeployment(toolDeployment);
        verify(lti).checkCompleteLTIRequest();
        verify(lti).setLoaded(true);
    }

    @Test
    public void testLoadLTIDataFromDBHandlesPartialRowGracefully() {
        Lti3Request lti = mock(Lti3Request.class);
        Query query = mock(Query.class);
        PlatformDeployment platformDeployment = PlatformDeployment.builder().keyId(1L).iss("iss1").clientId("client1").build();
        ToolDeployment toolDeployment = ToolDeployment.builder().deploymentId(10L).ltiDeploymentId("dep1").platformDeployment(platformDeployment).build();
        LtiContextEntity context = new LtiContextEntity("ctx1", toolDeployment, "Context Title", "json");

        // only k and c are present in the row (as if the left joins for l, m, u, t resolved to nothing)
        Object[] row = new Object[] {platformDeployment, context};

        doReturn("dep1").when(lti).getLtiDeploymentId();
        doReturn("aud1").when(lti).getAud();
        doReturn(query).when(entityManager).createQuery(anyString());
        doReturn(Collections.singletonList(row)).when(query).getResultList();
        doReturn(true).when(lti).isLoaded();

        boolean result = ltiDataService.loadLTIDataFromDB(lti, "link1");

        assertTrue(result);
        verify(lti).setKey(platformDeployment);
        verify(lti).setContext(context);
        verify(lti, never()).setLink(any());
        verify(lti, never()).setMembership(any());
        verify(lti, never()).setUser(any());
        verify(lti, never()).setToolDeployment(any());
    }

    // ---------------------------------------------------------------------
    // upsertLTIDataInDB - shared baseline representing a request whose context, link, user
    // and membership are all already attached and already match the request's own values
    // (so, by default, the baseline should insert and update nothing)
    // ---------------------------------------------------------------------

    private PlatformDeployment platformDeployment;
    private ToolDeployment toolDeployment;
    private LtiContextEntity context;
    private LtiLinkEntity link;
    private LtiUserEntity user;
    private LtiMembershipEntity membership;

    private Lti3Request mockBaselineLti() {
        platformDeployment = PlatformDeployment.builder().keyId(1L).iss("iss1").clientId("client1").build();
        toolDeployment = ToolDeployment.builder().deploymentId(10L).ltiDeploymentId("dep1").platformDeployment(platformDeployment).build();
        context = new LtiContextEntity("ctx1", toolDeployment, "Context Title", "https://example.com/memberships", "https://example.com/lineitems", "json");
        link = new LtiLinkEntity("link1", context, "Link Title");
        user = new LtiUserEntity("user1", new Date(), platformDeployment);
        user.setDisplayName("User Name");
        user.setEmail("user@example.com");
        user.setLmsUserId("canvasUser1");
        membership = new LtiMembershipEntity(context, user, 1);

        Lti3Request lti = mock(Lti3Request.class);

        doReturn(toolDeployment).when(lti).getToolDeployment();
        doReturn(platformDeployment).when(lti).getKey();
        doReturn("dep1").when(lti).getLtiDeploymentId();

        doReturn(context).when(lti).getContext();
        doReturn(context.getContextKey()).when(lti).getLtiContextId();
        doReturn(context.getTitle()).when(lti).getLtiContextTitle();
        doReturn(context.getContext_memberships_url()).when(lti).getLtiNamesRoleServiceContextMembershipsUrl();
        doReturn(context.getLineitems()).when(lti).getLtiEndpointLineItems();

        doReturn(link).when(lti).getLink();
        doReturn(link.getLinkKey()).when(lti).getLtiLinkId();
        doReturn(link.getTitle()).when(lti).getLtiLinkTitle();

        doReturn(user).when(lti).getUser();
        doReturn(user.getUserKey()).when(lti).getSub();
        doReturn(user.getDisplayName()).when(lti).getLtiName();
        doReturn(user.getEmail()).when(lti).getLtiEmail();

        Map<String, Object> custom = new HashMap<>();
        custom.put("canvas_user_id", user.getLmsUserId());
        doReturn(custom).when(lti).getLtiCustom();

        doReturn(membership).when(lti).getMembership();
        doReturn(List.of("Instructor")).when(lti).getLtiRoles();
        doReturn(membership.getRole()).when(lti).getUserRoleNumber();

        doReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK).when(lti).getLtiMessageType();
        doReturn("true").when(lti).checkCompleteLTIRequest();
        doReturn("true").when(lti).checkCompleteDeepLinkingRequest();
        doReturn(0).when(lti).getLoadingUpdates();

        return lti;
    }

    @Test
    public void testUpsertLTIDataInDBThrowsWhenToolDeploymentIsNull() {
        Lti3Request lti = mockBaselineLti();

        DataServiceException exception = assertThrows(
            DataServiceException.class,
            () -> ltiDataService.upsertLTIDataInDB(lti, null, "link1")
        );

        assertEquals("ToolDeployment data must not be null to update data", exception.getMessage());
    }

    @Test
    public void testUpsertLTIDataInDBReconnectsEverythingAndUpdatesNothingWhenAlreadyComplete() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        ReflectionTestUtils.setField(ltiDataService, "ltiDataVerboseLoggingEnabled", true);

        int result = ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        assertEquals(0, result);
        verify(lti).setLoadingUpdates(0);
        verify(lti).setUpdated(true);
        verify(entityManager).merge(context);
        verify(entityManager).merge(link);
        verify(entityManager).merge(user);
        verify(entityManager).merge(membership);
        verify(ltiContextRepository, never()).save(any());
        verify(ltiLinkRepository, never()).save(any());
        verify(ltiUserRepository, never()).save(any());
        verify(ltiMembershipRepository, never()).save(any());
    }

    @Test
    public void testUpsertLTIDataInDBSetsToolDeploymentWhenMissingFromRequest() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null).when(lti).getToolDeployment();

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        verify(lti).setToolDeployment(toolDeployment);
    }

    @Test
    public void testUpsertLTIDataInDBInsertsNewContextWhenNotFoundInRepository() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        // null on the first call (to trigger the not-found/insert branch), then the resolved
        // context afterward - lti is a plain mock, so it won't reflect what setContext(...) was
        // called with on its own, but production code re-reads getContext() later in the method
        doReturn(null, context).when(lti).getContext();
        doReturn(null).when(ltiContextRepository).findByContextKeyAndToolDeployment("ctx1", toolDeployment);
        doReturn(context).when(ltiContextRepository).save(any(LtiContextEntity.class));
        doReturn(1).when(lti).getLoadingUpdates();

        int result = ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        assertEquals(1, result);
        ArgumentCaptor<LtiContextEntity> captor = ArgumentCaptor.forClass(LtiContextEntity.class);
        verify(ltiContextRepository).save(captor.capture());
        assertEquals("ctx1", captor.getValue().getContextKey());
        verify(lti).setContext(context);
        verify(lti).setLoadingUpdates(1);
    }

    @Test
    public void testUpsertLTIDataInDBReconnectsExistingContextFoundInRepository() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null, context).when(lti).getContext();
        doReturn(context).when(ltiContextRepository).findByContextKeyAndToolDeployment("ctx1", toolDeployment);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        verify(ltiContextRepository, never()).save(any());
        verify(lti).setContext(context);
        // called once from the context-resolution block itself, and again from the later
        // membership-reconnect block (baseline membership stays non-null throughout)
        verify(lti, times(2)).setLtiContextId(context.getContextKey());
    }

    @Test
    public void testUpsertLTIDataInDBInsertsNewLinkWithDefaultTitleAndParsesLinkFromTargetUrl() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null).when(lti).getLink();
        doReturn("https://example.com/launch?link=9999").when(lti).getLtiTargetLinkUrl();
        doReturn(Collections.emptyList()).when(ltiLinkRepository).findByLinkKeyAndContext(eq("9999"), any());
        doReturn(link).when(ltiLinkRepository).save(any(LtiLinkEntity.class));
        doReturn(1).when(lti).getLoadingUpdates();

        int result = ltiDataService.upsertLTIDataInDB(lti, toolDeployment, null);

        assertEquals(1, result);
        ArgumentCaptor<LtiLinkEntity> captor = ArgumentCaptor.forClass(LtiLinkEntity.class);
        verify(ltiLinkRepository).save(captor.capture());
        assertEquals("9999", captor.getValue().getLinkKey());
        assertEquals(link.getTitle(), captor.getValue().getTitle());
    }

    @Test
    public void testUpsertLTIDataInDBInsertsNewLinkWithHardcodedTitleFor1234() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null).when(lti).getLink();
        doReturn(Collections.emptyList()).when(ltiLinkRepository).findByLinkKeyAndContext(eq("1234"), any());
        doReturn(link).when(ltiLinkRepository).save(any(LtiLinkEntity.class));

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "1234");

        ArgumentCaptor<LtiLinkEntity> captor = ArgumentCaptor.forClass(LtiLinkEntity.class);
        verify(ltiLinkRepository).save(captor.capture());
        assertEquals("My Test Link", captor.getValue().getTitle());
    }

    @Test
    public void testUpsertLTIDataInDBInsertsNewLinkWithHardcodedTitleFor4567() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null).when(lti).getLink();
        doReturn(Collections.emptyList()).when(ltiLinkRepository).findByLinkKeyAndContext(eq("4567"), any());
        doReturn(link).when(ltiLinkRepository).save(any(LtiLinkEntity.class));

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "4567");

        ArgumentCaptor<LtiLinkEntity> captor = ArgumentCaptor.forClass(LtiLinkEntity.class);
        verify(ltiLinkRepository).save(captor.capture());
        assertEquals("Another Link", captor.getValue().getTitle());
    }

    @Test
    public void testUpsertLTIDataInDBReconnectsExistingLinkFoundInRepository() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null, link).when(lti).getLink();
        doReturn(List.of(link)).when(ltiLinkRepository).findByLinkKeyAndContext(eq("link1"), any());

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        verify(ltiLinkRepository, never()).save(any());
        verify(lti).setLink(link);
        verify(lti).setLtiLinkId(link.getLinkKey());
    }

    @Test
    public void testUpsertLTIDataInDBInsertsNewUserWithCanvasUserIdCustomClaim() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null, user).when(lti).getUser();
        doReturn(null).when(ltiUserRepository).findFirstByUserKeyAndPlatformDeployment("user1", platformDeployment);
        doReturn(user).when(ltiUserRepository).save(any(LtiUserEntity.class));

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        ArgumentCaptor<LtiUserEntity> captor = ArgumentCaptor.forClass(LtiUserEntity.class);
        verify(ltiUserRepository).save(captor.capture());
        assertEquals("user1", captor.getValue().getUserKey());
        assertEquals("canvasUser1", captor.getValue().getLmsUserId());
    }

    @Test
    public void testUpsertLTIDataInDBInsertsNewUserWithoutCanvasUserIdCustomClaim() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null, user).when(lti).getUser();
        doReturn(new HashMap<>()).when(lti).getLtiCustom();
        doReturn(null).when(ltiUserRepository).findFirstByUserKeyAndPlatformDeployment("user1", platformDeployment);
        doReturn(user).when(ltiUserRepository).save(any(LtiUserEntity.class));

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        ArgumentCaptor<LtiUserEntity> captor = ArgumentCaptor.forClass(LtiUserEntity.class);
        verify(ltiUserRepository).save(captor.capture());
        assertNull(captor.getValue().getLmsUserId());
    }

    @Test
    public void testUpsertLTIDataInDBReconnectsExistingUserFoundInRepository() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null, user).when(lti).getUser();
        doReturn(user).when(ltiUserRepository).findFirstByUserKeyAndPlatformDeployment("user1", platformDeployment);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        verify(ltiUserRepository, never()).save(any());
        verify(lti).setUser(user);
        // called once from the user-reconnect block itself, and again from the later
        // membership-reconnect block (baseline membership stays non-null throughout)
        verify(lti, times(2)).setSub(user.getUserKey());
        verify(lti).setLtiName(user.getDisplayName());
        verify(lti).setLtiEmail(user.getEmail());
    }

    @Test
    public void testUpsertLTIDataInDBInsertsNewMembershipWhenNotFoundInRepository() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null).when(lti).getMembership();
        doReturn(null).when(ltiMembershipRepository).findByUserAndContext(user, context);
        doReturn(membership).when(ltiMembershipRepository).save(any(LtiMembershipEntity.class));
        doReturn(1).when(lti).makeUserRoleNum(List.of("Instructor"));

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        ArgumentCaptor<LtiMembershipEntity> captor = ArgumentCaptor.forClass(LtiMembershipEntity.class);
        verify(ltiMembershipRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
        assertEquals(context, captor.getValue().getContext());
        verify(lti).setMembership(membership);
    }

    @Test
    public void testUpsertLTIDataInDBReconnectsExistingMembershipFoundInRepository() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(null).when(lti).getMembership();
        doReturn(membership).when(ltiMembershipRepository).findByUserAndContext(user, context);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        verify(ltiMembershipRepository, never()).save(any());
        verify(lti).setMembership(membership);
        // called once from the user-reconnect block and once from the membership-reconnect
        // block (baseline context/user stay non-null throughout, so both blocks run)
        verify(lti, times(2)).setSub(user.getUserKey());
        verify(lti, times(2)).setLtiContextId(context.getContextKey());
    }

    @Test
    public void testUpsertLTIDataInDBUpdatesContextTitleWhenChanged() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn("New Context Title").when(lti).getLtiContextTitle();
        doReturn(context).when(ltiContextRepository).save(context);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        // the context-already-present resolution branch eagerly syncs the title onto the entity
        // before the later diff-detection "next we handle updates" block ever runs, so by the time
        // that block checks for a title difference there is none left to find - the title still
        // ends up correct, just via entityManager.merge() rather than ltiContextRepository.save()
        assertEquals("New Context Title", context.getTitle());
        verify(ltiContextRepository, never()).save(any());
        verify(lti).setLoadingUpdates(0);
    }

    @Test
    public void testUpsertLTIDataInDBUpdatesLinkTitleWhenChanged() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn("New Link Title").when(lti).getLtiLinkTitle();
        doReturn(link).when(ltiLinkRepository).save(link);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        assertEquals("New Link Title", link.getTitle());
        verify(ltiLinkRepository).save(link);
        verify(lti).setLoadingUpdates(1);
    }

    @Test
    public void testUpsertLTIDataInDBUpdatesUserWhenDisplayNameChanged() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn("New Name").when(lti).getLtiName();
        doReturn(user).when(ltiUserRepository).save(user);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        assertEquals("New Name", user.getDisplayName());
        verify(ltiUserRepository).save(user);
        verify(lti).setLoadingUpdates(1);
    }

    @Test
    public void testUpsertLTIDataInDBUpdatesUserWhenEmailChanged() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn("new@example.com").when(lti).getLtiEmail();
        doReturn(user).when(ltiUserRepository).save(user);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        assertEquals("new@example.com", user.getEmail());
        verify(ltiUserRepository).save(user);
    }

    @Test
    public void testUpsertLTIDataInDBUpdatesUserWhenCanvasUserIdChanged() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        Map<String, Object> custom = new HashMap<>();
        custom.put("canvas_user_id", "newCanvasUser");
        doReturn(custom).when(lti).getLtiCustom();
        doReturn(user).when(ltiUserRepository).save(user);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        assertEquals("newCanvasUser", user.getLmsUserId());
        verify(ltiUserRepository).save(user);
    }

    @Test
    public void testUpsertLTIDataInDBUpdatesMembershipWhenRoleChanged() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(2).when(lti).getUserRoleNumber();
        doReturn(membership).when(ltiMembershipRepository).save(membership);

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        assertEquals(2, membership.getRole());
        verify(ltiMembershipRepository).save(membership);
        verify(lti).setLoadingUpdates(1);
    }

    @Test
    public void testUpsertLTIDataInDBUsesDeepLinkingCompletenessCheckForNonResourceLinkMessages() throws DataServiceException {
        Lti3Request lti = mockBaselineLti();
        doReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING).when(lti).getLtiMessageType();
        doReturn("true").when(lti).checkCompleteDeepLinkingRequest();

        ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1");

        verify(lti).checkCompleteDeepLinkingRequest();
        verify(lti, never()).checkCompleteLTIRequest();
    }

    @Test
    public void testUpsertLTIDataInDBThrowsWhenLtiRequestIsIncomplete() {
        Lti3Request lti = mockBaselineLti();
        doReturn(" User (sub) is empty.\n ").when(lti).checkCompleteLTIRequest();

        DataServiceException exception = assertThrows(
            DataServiceException.class,
            () -> ltiDataService.upsertLTIDataInDB(lti, toolDeployment, "link1")
        );

        assertTrue(exception.getMessage().contains("LTI object is incomplete"));
    }

    // ---------------------------------------------------------------------
    // simple passthrough / accessor methods
    // ---------------------------------------------------------------------

    @Test
    public void testFindByUserKeyAndPlatformDeploymentDelegatesToRepository() {
        LtiUserEntity expected = new LtiUserEntity("user1", new Date(), null);
        PlatformDeployment pd = PlatformDeployment.builder().keyId(1L).build();
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment("user1", pd)).thenReturn(expected);

        LtiUserEntity result = ltiDataService.findByUserKeyAndPlatformDeployment("user1", pd);

        assertEquals(expected, result);
    }

    @Test
    public void testSaveLtiUserEntityDelegatesToRepository() {
        LtiUserEntity toSave = new LtiUserEntity("user1", new Date(), null);
        when(ltiUserRepository.save(toSave)).thenReturn(toSave);

        LtiUserEntity result = ltiDataService.saveLtiUserEntity(toSave);

        assertEquals(toSave, result);
        verify(ltiUserRepository).save(toSave);
    }

    @Test
    public void testFindByUserAndContextDelegatesToRepository() {
        LtiUserEntity u = new LtiUserEntity("user1", new Date(), null);
        LtiContextEntity c = new LtiContextEntity("ctx1", ToolDeployment.builder().build(), "title", "json");
        LtiMembershipEntity expected = new LtiMembershipEntity(c, u, 1);
        when(ltiMembershipRepository.findByUserAndContext(u, c)).thenReturn(expected);

        LtiMembershipEntity result = ltiDataService.findByUserAndContext(u, c);

        assertEquals(expected, result);
    }

    @Test
    public void testSaveLtiMembershipEntityDelegatesToRepository() {
        LtiUserEntity u = new LtiUserEntity("user1", new Date(), null);
        LtiContextEntity c = new LtiContextEntity("ctx1", ToolDeployment.builder().build(), "title", "json");
        LtiMembershipEntity toSave = new LtiMembershipEntity(c, u, 1);
        when(ltiMembershipWriteService.insert(toSave)).thenReturn(toSave);

        LtiMembershipEntity result = ltiDataService.saveLtiMembershipEntity(toSave);

        assertEquals(toSave, result);
    }

    // a real LTI launch and a roster sync can race to create the same (user_id, context_id)
    // membership - whichever loses the unique-constraint race must use the winner's row rather
    // than failing whatever it was part of.
    @Test
    public void testSaveLtiMembershipEntityReturnsExistingRowOnDuplicateKeyConflict() {
        LtiUserEntity u = new LtiUserEntity("user1", new Date(), null);
        LtiContextEntity c = new LtiContextEntity("ctx1", ToolDeployment.builder().build(), "title", "json");
        LtiMembershipEntity toSave = new LtiMembershipEntity(c, u, 1);
        LtiMembershipEntity winner = new LtiMembershipEntity(c, u, 1);
        when(ltiMembershipWriteService.insert(toSave)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));
        when(ltiMembershipRepository.findByUserAndContext(u, c)).thenReturn(winner);

        LtiMembershipEntity result = ltiDataService.saveLtiMembershipEntity(toSave);

        assertEquals(winner, result);
    }

    @Test
    public void testSaveLtiMembershipEntityReturnsExistingRowOnLockWaitTimeout() {
        LtiUserEntity u = new LtiUserEntity("user1", new Date(), null);
        LtiContextEntity c = new LtiContextEntity("ctx1", ToolDeployment.builder().build(), "title", "json");
        LtiMembershipEntity toSave = new LtiMembershipEntity(c, u, 1);
        LtiMembershipEntity winner = new LtiMembershipEntity(c, u, 1);
        when(ltiMembershipWriteService.insert(toSave)).thenThrow(new org.springframework.dao.CannotAcquireLockException("lock wait timeout"));
        when(ltiMembershipRepository.findByUserAndContext(u, c)).thenReturn(winner);

        LtiMembershipEntity result = ltiDataService.saveLtiMembershipEntity(toSave);

        assertEquals(winner, result);
    }

    // if the conflict wasn't actually caused by a concurrent winner (e.g. a genuinely different
    // problem), rethrow rather than silently returning null and pushing an NPE onto the caller.
    @Test
    public void testSaveLtiMembershipEntityRethrowsWhenNoExistingRowFound() {
        LtiUserEntity u = new LtiUserEntity("user1", new Date(), null);
        LtiContextEntity c = new LtiContextEntity("ctx1", ToolDeployment.builder().build(), "title", "json");
        LtiMembershipEntity toSave = new LtiMembershipEntity(c, u, 1);
        when(ltiMembershipWriteService.insert(toSave)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));
        when(ltiMembershipRepository.findByUserAndContext(u, c)).thenReturn(null);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> ltiDataService.saveLtiMembershipEntity(toSave));
    }

    @Test
    public void testFindAllByUserKeysAndPlatformDeploymentDelegatesToRepository() {
        LtiUserEntity expected = new LtiUserEntity("user1", new Date(), null);
        PlatformDeployment pd = PlatformDeployment.builder().keyId(1L).build();
        List<String> userKeys = List.of("user1", "user2");
        when(ltiUserRepository.findAllByUserKeyInAndPlatformDeployment(userKeys, pd)).thenReturn(List.of(expected));

        List<LtiUserEntity> result = ltiDataService.findAllByUserKeysAndPlatformDeployment(userKeys, pd);

        assertEquals(List.of(expected), result);
    }

    @Test
    public void testFindAllByUsersAndContextDelegatesToRepository() {
        LtiUserEntity u = new LtiUserEntity("user1", new Date(), null);
        LtiContextEntity c = new LtiContextEntity("ctx1", ToolDeployment.builder().build(), "title", "json");
        LtiMembershipEntity expected = new LtiMembershipEntity(c, u, 1);
        List<LtiUserEntity> users = List.of(u);
        when(ltiMembershipRepository.findByUserInAndContext(users, c)).thenReturn(List.of(expected));

        List<LtiMembershipEntity> result = ltiDataService.findAllByUsersAndContext(users, c);

        assertEquals(List.of(expected), result);
    }

    @Test
    public void testFindOrCreateToolDeploymentReturnsExistingToolDeploymentWhenFound() {
        ToolDeployment existing = ToolDeployment.builder().deploymentId(1L).ltiDeploymentId("dep1").build();
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId("iss1", "client1", "dep1"))
            .thenReturn(List.of(existing));

        ToolDeployment result = ltiDataService.findOrCreateToolDeployment("iss1", "client1", "dep1");

        assertEquals(existing, result);
        verify(platformDeploymentRepository, never()).findByIssAndClientId(anyString(), anyString());
    }

    @Test
    public void testFindOrCreateToolDeploymentReturnsNullWhenNoToolDeploymentAndNoPlatformDeploymentFound() {
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId("iss1", "client1", "dep1"))
            .thenReturn(Collections.emptyList());
        when(platformDeploymentRepository.findByIssAndClientId("iss1", "client1")).thenReturn(Collections.emptyList());

        ToolDeployment result = ltiDataService.findOrCreateToolDeployment("iss1", "client1", "dep1");

        assertNull(result);
        verify(toolDeploymentRepository, never()).save(any());
    }

    @Test
    public void testFindOrCreateToolDeploymentCreatesNewToolDeploymentWhenAutomaticDeploymentsEnabled() {
        PlatformDeployment pd = PlatformDeployment.builder().keyId(1L).iss("iss1").clientId("client1").enableAutomaticDeployments(true).build();
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId("iss1", "client1", "dep1"))
            .thenReturn(Collections.emptyList());
        when(platformDeploymentRepository.findByIssAndClientId("iss1", "client1")).thenReturn(List.of(pd));
        when(toolDeploymentRepository.save(any(ToolDeployment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ToolDeployment result = ltiDataService.findOrCreateToolDeployment("iss1", "client1", "dep1");

        assertEquals("dep1", result.getLtiDeploymentId());
        assertEquals(pd, result.getPlatformDeployment());
        verify(toolDeploymentRepository).save(any(ToolDeployment.class));
    }

    @Test
    public void testFindOrCreateToolDeploymentReturnsNullWhenAutomaticDeploymentsDisabled() {
        PlatformDeployment pd = PlatformDeployment.builder().keyId(1L).iss("iss1").clientId("client1").enableAutomaticDeployments(false).build();
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId("iss1", "client1", "dep1"))
            .thenReturn(Collections.emptyList());
        when(platformDeploymentRepository.findByIssAndClientId("iss1", "client1")).thenReturn(List.of(pd));

        ToolDeployment result = ltiDataService.findOrCreateToolDeployment("iss1", "client1", "dep1");

        assertNull(result);
        verify(toolDeploymentRepository, never()).save(any());
    }

    @Test
    public void testGetSetOwnPrivateKeyRoundTrip() {
        ltiDataService.setOwnPrivateKey("private-key-value");

        assertEquals("private-key-value", ltiDataService.getOwnPrivateKey());
    }

    @Test
    public void testGetSetOwnPublicKeyRoundTrip() {
        ltiDataService.setOwnPublicKey("public-key-value");

        assertEquals("public-key-value", ltiDataService.getOwnPublicKey());
    }

    @Test
    public void testGetSetDemoModeRoundTrip() {
        ltiDataService.setDemoMode(true);

        assertTrue(ltiDataService.getDemoMode());

        ltiDataService.setDemoMode(false);

        assertFalse(ltiDataService.getDemoMode());
    }

    @Test
    public void testGetPlatformDeploymentRepositoryReturnsInjectedRepository() {
        assertEquals(platformDeploymentRepository, ltiDataService.getPlatformDeploymentRepository());
    }

}
