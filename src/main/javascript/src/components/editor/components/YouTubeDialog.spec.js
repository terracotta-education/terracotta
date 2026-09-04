import { DOMWrapper } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import YouTubeDialog from "./YouTubeDialog.vue";

const IFRAME_EMBED =
  '<iframe width="640" height="360" src="https://www.youtube.com/embed/abc123" title="YouTube video"></iframe>';

// v-dialog content is teleported to document.body (outside the wrapper's own
// element), so we look it up via a DOMWrapper around document.body and
// unmount after every test to avoid leaking teleported nodes across tests.
let wrapper;

afterEach(() => {
  wrapper?.unmount();
  wrapper = undefined;
});

const body = () => new DOMWrapper(document.body);

const mountDialog = props => {
  wrapper = mountComponent(YouTubeDialog, {
    props: {
      editor: {},
      ...props
    }
  });

  return wrapper;
};

describe("YouTubeDialog", () => {
  it("renders with the dialog open by default", () => {
    mountDialog();

    expect(wrapper.findComponent({ name: "VDialog" }).exists()).toBe(true);
    expect(body().find(".input-embed-code").exists()).toBe(true);
  });

  it("pre-fills the textarea from the embedCode prop", () => {
    mountDialog({ embedCode: "some embed code" });

    expect(body().find("textarea").element.value).toBe("some embed code");
  });

  it("disables the Add button when the embed code is empty", () => {
    mountDialog();

    const buttons = body().findAll("button").filter(button => button.text() === "Add");

    expect(buttons[0].attributes("disabled")).not.toBeUndefined();
  });

  it("enables the Add button once embed code is entered and emits submit with parsed iframe attributes", async () => {
    mountDialog();

    await body().find("textarea").setValue(IFRAME_EMBED);

    const addButton = body().findAll("button").find(button => button.text() === "Add");

    expect(addButton.attributes("disabled")).toBeUndefined();

    await addButton.trigger("click");

    const submitted = wrapper.emitted("submit");

    expect(submitted).toBeTruthy();
    expect(submitted[0][0]).toEqual({
      src: "https://youtu.be/abc123",
      height: 360,
      width: 640
    });
  });

  it("falls back to default width/height when the iframe has no dimensions", async () => {
    mountDialog();

    await body()
      .find("textarea")
      .setValue('<iframe src="https://www.youtube.com/embed/xyz789"></iframe>');

    const addButton = body().findAll("button").find(button => button.text() === "Add");

    await addButton.trigger("click");

    expect(wrapper.emitted("submit")[0][0]).toEqual({
      src: "https://youtu.be/xyz789",
      height: 315,
      width: 560
    });
  });

  it("uses the raw embed code as src when it is not an iframe/youtube URL", async () => {
    mountDialog();

    await body().find("textarea").setValue("plain text, not an iframe");

    const addButton = body().findAll("button").find(button => button.text() === "Add");

    await addButton.trigger("click");

    expect(wrapper.emitted("submit")[0][0].src).toBe("plain text, not an iframe");
  });

  it("emits close when the close icon button is clicked", async () => {
    mountDialog();

    const closeIconButton = body().find(".mdi-close").element.closest("button");

    closeIconButton.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("close")).toBeTruthy();
  });

  it("emits close when the Close text button is clicked", async () => {
    mountDialog();

    const closeButton = body().findAll("button").find(button => button.text() === "Close");

    await closeButton.trigger("click");

    expect(wrapper.emitted("close")).toBeTruthy();
  });
});
