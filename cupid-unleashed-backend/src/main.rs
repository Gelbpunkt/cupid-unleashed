use std::{fs, io, thread::sleep, time::Duration};

use android_logger::Config;
use android_system_properties::AndroidSystemProperties;
use log::LevelFilter;

/// Path for the mi_thermald profile config.
const MI_THERMALD_SCONFIG: &str = "/sys/devices/virtual/thermal/thermal_message/sconfig";

/// Benchmark mode.
const MI_THERMALD_BENCHMARK: &[u8] = b"10";
/// mgame mode.
const MI_THERMALD_MGAME: &[u8] = b"20";

/// Efficiency underclock values.
/// CPU 0-3 are little, 4-6 are big and 7 is prime.

/// scaling_available_frequencies:
/// 307200 403200 518400 614400 729600 844800 960000 1075200 1171200 1267200
/// 1363200 1478400 1574400 1689600 1785600
const EFFICIENCY_UC_LITTLE: &str = "1075200";
/// scaling_available_frequencies:
/// 633600 768000 883200 998400 1113600 1209600 1324800 1440000 1555200 1651200
/// 1766400 1881600 1996800 2112000 2227200 2342400 2419200
const EFFICIENCY_UC_BIG: &str = "1555200";
/// scaling_available_frequencies:
/// 806400 940800 1056000 1171200 1286400 1401600 1497600 1612800 1728000
/// 1843200 1958400 2054400 2169600 2284800 2400000 2515200 2630400 2726400
/// 2822400 2841600
const EFFICIENCY_UC_PRIME: &str = "2054400";

const UNLEASHED_LITTLE: &str = "1785600";
const UNLEASHED_BIG: &str = "2419200";
const UNLEASHED_PRIME: &str = "2841600";

const LITTLE_CORE_SCALING_MAX_FREQ_PATHS: &[&str] = &[
    "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq",
    "/sys/devices/system/cpu/cpu1/cpufreq/scaling_max_freq",
    "/sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq",
    "/sys/devices/system/cpu/cpu3/cpufreq/scaling_max_freq",
];
const BIG_CORE_SCALING_MAX_FREQ_PATHS: &[&str] = &[
    "/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq",
    "/sys/devices/system/cpu/cpu5/cpufreq/scaling_max_freq",
    "/sys/devices/system/cpu/cpu6/cpufreq/scaling_max_freq",
];
const PRIME_CORE_SCALING_MAX_FREQ_PATH: &str =
    "/sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq";

fn apply_freqs(little: &str, big: &str, prime: &str) -> Result<(), io::Error> {
    for path in LITTLE_CORE_SCALING_MAX_FREQ_PATHS {
        fs::write(path, little)?;
    }

    for path in BIG_CORE_SCALING_MAX_FREQ_PATHS {
        fs::write(path, big)?;
    }

    fs::write(PRIME_CORE_SCALING_MAX_FREQ_PATH, prime)
}

fn apply_efficiency_uc() -> Result<(), io::Error> {
    apply_freqs(EFFICIENCY_UC_LITTLE, EFFICIENCY_UC_BIG, EFFICIENCY_UC_PRIME)
}

fn apply_unleashed() -> Result<(), io::Error> {
    apply_freqs(UNLEASHED_LITTLE, UNLEASHED_BIG, UNLEASHED_PRIME)
}

/// Which mode the CPU should be set to.
#[derive(Debug, PartialEq)]
enum Mode {
    /// Highest performance and thermal limits at advertised clock speeds.
    Benchmark,
    /// High performance and reasonable thermal limits at advertised clock
    /// speeds.
    Advertised,
    /// Average performance at underclocked speeds.
    Efficiency,
}

impl Mode {
    fn parse_from_props(props: &AndroidSystemProperties) -> Self {
        let mode_str = props.get("persist.unleashed.mode");
        let mode = mode_str.as_deref().unwrap_or("advertised");

        match mode {
            "benchmark" => Self::Benchmark,
            "advertised" => Self::Advertised,
            "efficiency" => Self::Efficiency,
            _ => Self::Advertised,
        }
    }

    fn apply(&self) -> Result<(), io::Error> {
        // Apply a mi_thermald profile according to this mode
        let thermald_mode: &[u8] = match self {
            Self::Benchmark => MI_THERMALD_BENCHMARK,
            _ => MI_THERMALD_MGAME,
        };

        fs::write(MI_THERMALD_SCONFIG, thermald_mode)?;

        // Optionally, apply an underclock
        if let Self::Efficiency = self {
            apply_efficiency_uc()
        } else {
            apply_unleashed()
        }
    }
}

const REFRESH_INTERVAL: Duration = Duration::from_secs(60);

fn main() {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Debug)
            .with_tag("unleashed"),
    );

    log::info!("Unleashed is initializing");

    let props = AndroidSystemProperties::new();
    let mut last_applied_mode = None;

    loop {
        // Refresh settings once per minute
        sleep(REFRESH_INTERVAL);

        let mode = Mode::parse_from_props(&props);

        // If this mode is already applied, do not do anything
        if last_applied_mode.as_ref() == Some(&mode) {
            continue;
        }

        if let Err(e) = mode.apply() {
            log::error!("Failed to apply: {e}");
        } else {
            log::info!("Successfully applied {mode:?}");

            last_applied_mode = Some(mode);
        }
    }
}
