import { afterEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

import { mountComponent } from "@/test-utils/mount";
import { container as useContainerStore } from "@/store/messaging/container.module";
import { message as useMessageStore } from "@/store/messaging/message.module";
import { participants as useParticipantsStore } from "@/store/participants.module";
import Preview from "./Preview.vue";

const baseProps = {
  experimentId: 1,
  exposureId: "exposure-1",
  containerId: "container-1",
  messageId: "message-1",
  contentId: "content-1"
};

const buildMessage = (overrides = {}) => ({
  id: "message-1",
  ruleSets: [],
  configuration: { type: "EMAIL" },
  content: { html: "<p>Hello</p>", pipedText: null },
  ...overrides
});

const setupStores = ({
  message: msg = buildMessage(),
  participants = []
} = {}) => {
  const pinia = createPinia();
  setActivePinia(pinia);

  useContainerStore().messageContainers = [
    { id: "container-1", messages: [msg] }
  ];
  useParticipantsStore().participants = participants;
  useParticipantsStore().fetchParticipants = vi.fn().mockResolvedValue(null);

  return pinia;
};

// The panel's body (participant list, preview body, refresh button) lives
// inside a collapsed v-expansion-panel-text, which Vuetify only renders once
// the panel has actually been opened.
const openPanel = async wrapper => {
  await wrapper.find(".v-expansion-panel-title").trigger("click");
  await flushPromises();
};

describe("Preview", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("renders nothing until initialization completes, then shows the panel", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(Preview, { props: baseProps, pinia });

    expect(wrapper.findComponent({ name: "VExpansionPanels" }).exists()).toBe(false);

    await flushPromises();

    expect(wrapper.findComponent({ name: "VExpansionPanels" }).exists()).toBe(true);
  });

  it("lists participants sorted alphabetically by display name", async () => {
    const pinia = setupStores({
      participants: [
        { id: 2, user: { displayName: "Zed" } },
        { id: 1, user: { displayName: "Anna" } }
      ]
    });

    wrapper = mountComponent(Preview, { props: baseProps, pinia });
    await flushPromises();
    await openPanel(wrapper);

    const items = wrapper.findAllComponents({ name: "VListItem" });
    expect(items.map(i => i.text())).toEqual(["Anna", "Zed"]);
  });

  it("shows the fallback message before any participant is selected", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(Preview, { props: baseProps, pinia });
    await flushPromises();
    await openPanel(wrapper);

    expect(wrapper.text()).toContain(
      "Please select a user to preview their message."
    );
  });

  it("fetches and displays the preview for the selected participant", async () => {
    const pinia = setupStores({
      participants: [{ id: 5, user: { displayName: "Anna" } }]
    });
    useMessageStore().fetchPreview = vi.fn().mockImplementation(async () => {
      useMessageStore().preview = { body: "<p>Hi Anna!</p>" };
      return useMessageStore().preview;
    });

    wrapper = mountComponent(Preview, { props: baseProps, pinia });
    await flushPromises();
    await openPanel(wrapper);

    await wrapper.findComponent({ name: "VListItem" }).trigger("click");
    await flushPromises();

    expect(useMessageStore().fetchPreview).toHaveBeenCalledWith([
      baseProps.experimentId,
      baseProps.exposureId,
      baseProps.containerId,
      baseProps.messageId,
      expect.objectContaining({ id: 5, body: "<p>Hello</p>" })
    ]);
    expect(wrapper.text()).toContain("Hi Anna!");
  });

  it("shows a refresh button once the message body changes after a preview was already fetched", async () => {
    const msg = buildMessage();
    const pinia = setupStores({
      message: msg,
      participants: [{ id: 5, user: { displayName: "Anna" } }]
    });
    useMessageStore().fetchPreview = vi.fn().mockImplementation(async () => {
      useMessageStore().preview = { body: "<p>Hi!</p>" };
      return useMessageStore().preview;
    });

    wrapper = mountComponent(Preview, { props: baseProps, pinia });
    await flushPromises();
    await openPanel(wrapper);

    await wrapper.findComponent({ name: "VListItem" }).trigger("click");
    await flushPromises();

    expect(wrapper.text()).not.toContain("Refresh");

    // Mutate through the store's own reactive reference (not the plain local
    // "msg" object) so Vue's reactivity actually picks up the change.
    const storedMessage = useContainerStore()
      .messageContainers.find(c => c.id === "container-1")
      .messages.find(m => m.id === "message-1");
    storedMessage.content.html = "<p>Updated</p>";
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Refresh");
  });
});
