import dayjs from "dayjs";
import { buildQuickPresets } from "./quickPresets";

describe("buildQuickPresets", () => {
  it("offers every shortcut first thing in the morning", () => {
    const presets = buildQuickPresets(dayjs("2030-01-01T07:00:00"));

    expect(presets.map((p) => p.label)).toEqual([
      "In 1 hour",
      "This evening",
      "Tomorrow 9 AM",
      "Next week",
    ]);
  });

  it("drops this evening once the evening has passed", () => {
    const presets = buildQuickPresets(dayjs("2030-01-01T21:00:00"));

    expect(presets.map((p) => p.label)).not.toContain("This evening");
  });

  it("never returns a time in the past", () => {
    const now = dayjs("2030-01-01T21:00:00");

    buildQuickPresets(now).forEach((preset) => {
      expect(preset.value.isAfter(now)).toBe(true);
    });
  });

  it("puts this evening at six o'clock", () => {
    const [, evening] = buildQuickPresets(dayjs("2030-01-01T07:00:00"));

    expect(evening.value.format("HH:mm")).toBe("18:00");
  });

  it("puts tomorrow's shortcut at nine the next day", () => {
    const presets = buildQuickPresets(dayjs("2030-01-01T07:00:00"));
    const tomorrow = presets.find((p) => p.label === "Tomorrow 9 AM");

    expect(tomorrow?.value.format("YYYY-MM-DD HH:mm")).toBe("2030-01-02 09:00");
  });

  it("puts next week a full seven days out", () => {
    const presets = buildQuickPresets(dayjs("2030-01-01T07:00:00"));
    const nextWeek = presets.find((p) => p.label === "Next week");

    expect(nextWeek?.value.format("YYYY-MM-DD HH:mm")).toBe("2030-01-08 09:00");
  });
});
