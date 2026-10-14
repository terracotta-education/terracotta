import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { getAuthorization, replace } = vi.hoisted(() => ({
  getAuthorization: vi.fn(),
  replace: vi.fn()
}));

vi.mock("@/services", () => ({
  lmsAuthorizationService: { getAuthorization }
}));

vi.mock("vue-router", () => ({
  useRouter: () => ({ replace })
}));

import { mountComponent } from "@/test-utils/mount";
import { api } from "@/store/api.module";
import { configuration } from "@/store/configuration.module";
import OAuth2Redirect from "./OAuth2Redirect.vue";

const AUTH_URL = "https://lms.example.com/oauth/authorize";

async function mountWithUrl() {
  const wrapper = mountComponent(OAuth2Redirect);

  api().$patch({ lmsApiOAuthURL: AUTH_URL });
  await wrapper.vm.$nextTick();

  return wrapper;
}

describe("OAuth2Redirect", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    getAuthorization.mockReset().mockResolvedValue({ authorized: false });
    replace.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it("falls back to a generic LMS title and no href when nothing is loaded yet", () => {
    const wrapper = mountComponent(OAuth2Redirect);

    expect(wrapper.text()).toContain("Terracotta wants to access your LMS account");
    expect(
      wrapper.find(".redirect-button").attributes("href")
    ).toBeFalsy();
  });

  it("renders the configured LMS title throughout the page", () => {
    const wrapper = mountComponent(OAuth2Redirect);

    const configurationStore = configuration();
    configurationStore.$patch({ configurations: { lmsTitle: "Canvas" } });

    return wrapper.vm.$nextTick().then(() => {
      expect(wrapper.text()).toContain("Terracotta wants to access your Canvas account");
      expect(wrapper.text()).toContain("Go to the Canvas Authorization Page");
    });
  });

  it("links the authorization button to the API store's OAuth URL", async () => {
    const wrapper = mountComponent(OAuth2Redirect);

    const apiStore = api();
    apiStore.$patch({ lmsApiOAuthURL: "https://lms.example.com/oauth/authorize" });

    await wrapper.vm.$nextTick();

    expect(wrapper.find(".redirect-button").attributes("href")).toBe(
      "https://lms.example.com/oauth/authorize"
    );
  });

  it("opens the link in a new tab rather than inside the LMS frame", async () => {
    const wrapper = await mountWithUrl();

    expect(wrapper.find(".redirect-button").attributes("target")).toBe("_blank");
  });

  it("opens authorization in its own window and waits there", async () => {
    const open = vi.spyOn(window, "open").mockReturnValue({});
    const wrapper = await mountWithUrl();
    const event = new MouseEvent("click", { cancelable: true });

    wrapper.find(".redirect-button").element.dispatchEvent(event);
    await wrapper.vm.$nextTick();

    expect(open).toHaveBeenCalledWith(AUTH_URL, "terracotta-lms-authorization", expect.stringContaining("popup"));
    expect(event.defaultPrevented).toBe(true);
    expect(wrapper.find(".authorization-waiting").exists()).toBe(true);
  });

  it("lets the link open a new tab when the popup is blocked, and still waits", async () => {
    vi.spyOn(window, "open").mockReturnValue(null);
    const wrapper = await mountWithUrl();
    const event = new MouseEvent("click", { cancelable: true });

    wrapper.find(".redirect-button").element.dispatchEvent(event);
    await wrapper.vm.$nextTick();

    expect(event.defaultPrevented).toBe(false);
    expect(wrapper.find(".authorization-waiting").exists()).toBe(true);
  });

  it("continues to Home once the server reports the instructor authorized", async () => {
    vi.spyOn(window, "open").mockReturnValue({});
    getAuthorization
      .mockResolvedValueOnce({ authorized: false })
      .mockResolvedValueOnce({ authorized: true });
    const wrapper = await mountWithUrl();

    await wrapper.find(".redirect-button").trigger("click");
    await vi.advanceTimersByTimeAsync(2000);

    expect(replace).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(2000);

    expect(getAuthorization).toHaveBeenCalledTimes(2);
    expect(api().lmsApiOAuthURL).toBe("");
    expect(replace).toHaveBeenCalledWith({ name: "Home" });
  });

  it("checks for authorization as soon as the page opens, without a click", async () => {
    // e.g. the instructor right-clicked the button and authorized in a new tab
    getAuthorization.mockResolvedValueOnce({ authorized: true });
    await mountWithUrl();

    await vi.advanceTimersByTimeAsync(2000);

    expect(getAuthorization).toHaveBeenCalledTimes(1);
    expect(replace).toHaveBeenCalledWith({ name: "Home" });
  });

  it("keeps a single poll running when the button is clicked again", async () => {
    vi.spyOn(window, "open").mockReturnValue({});
    const wrapper = await mountWithUrl();

    await wrapper.find(".redirect-button").trigger("click");
    await wrapper.find(".redirect-button").trigger("click");
    await vi.advanceTimersByTimeAsync(2000);

    expect(getAuthorization).toHaveBeenCalledTimes(1);
  });

  it("stops checking fifteen minutes after the last click", async () => {
    vi.spyOn(window, "open").mockReturnValue({});
    const wrapper = await mountWithUrl();

    await vi.advanceTimersByTimeAsync(10 * 60 * 1000);
    await wrapper.find(".redirect-button").trigger("click");
    // the click restarted the window: still waiting past fifteen minutes from mount
    await vi.advanceTimersByTimeAsync(10 * 60 * 1000);

    expect(wrapper.find(".authorization-waiting").exists()).toBe(true);

    await vi.advanceTimersByTimeAsync(5 * 60 * 1000 + 2000);
    const calls = getAuthorization.mock.calls.length;
    await vi.advanceTimersByTimeAsync(10000);

    expect(getAuthorization).toHaveBeenCalledTimes(calls);
    expect(wrapper.find(".authorization-waiting").exists()).toBe(false);
  });

  it("stops checking fifteen minutes after the page opens when nothing is clicked", async () => {
    await mountWithUrl();

    await vi.advanceTimersByTimeAsync(15 * 60 * 1000 + 2000);
    const calls = getAuthorization.mock.calls.length;
    await vi.advanceTimersByTimeAsync(10000);

    expect(getAuthorization).toHaveBeenCalledTimes(calls);
  });

  it("stops polling when the page is left", async () => {
    const wrapper = await mountWithUrl();

    wrapper.unmount();
    await vi.advanceTimersByTimeAsync(10000);

    expect(getAuthorization).not.toHaveBeenCalled();
  });

  it("does nothing before the authorization URL is loaded", async () => {
    const open = vi.spyOn(window, "open");
    const wrapper = mountComponent(OAuth2Redirect);

    await wrapper.find(".redirect-button").trigger("click");

    expect(open).not.toHaveBeenCalled();
    expect(wrapper.find(".authorization-waiting").exists()).toBe(false);
  });

  it("renders a tooltip explaining each requested permission", () => {
    const wrapper = mountComponent(OAuth2Redirect);

    const tooltips = wrapper.findAllComponents({ name: "ToolTip" });

    expect(tooltips).toHaveLength(2);
    expect(wrapper.text()).toContain("List assignment submissions");
    expect(wrapper.text()).toContain(
      "Create, list, edit and delete assignments"
    );
  });
});
