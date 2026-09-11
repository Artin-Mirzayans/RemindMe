import dayjs, { Dayjs } from "dayjs";

export interface QuickPreset {
    label: string;
    value: Dayjs;
}

const at = (base: Dayjs, hour: number) =>
    base.hour(hour).minute(0).second(0).millisecond(0);

export const buildQuickPresets = (now: Dayjs = dayjs()): QuickPreset[] => {
    const presets: QuickPreset[] = [
        { label: "In 1 hour", value: now.add(1, "hour").second(0).millisecond(0) },
        { label: "This evening", value: at(now, 18) },
        { label: "Tomorrow 9 AM", value: at(now.add(1, "day"), 9) },
        { label: "Next week", value: at(now.add(7, "day"), 9) },
    ];

    return presets.filter((preset) => preset.value.isAfter(now));
};
