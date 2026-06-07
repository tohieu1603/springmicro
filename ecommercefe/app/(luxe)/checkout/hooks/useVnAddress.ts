"use client";

import { useCallback, useEffect, useState } from "react";

/**
 * Cascading Vietnamese administrative-division loader, backed by the public
 * provinces.open-api.vn dataset. Three independent fetches:
 *
 *   – {@link loadProvinces} fires on mount, returns the 63 provinces sorted by
 *     name. Cached on the module to avoid re-downloading on every open.
 *   – {@link selectProvince} re-fetches the province with depth=2 to pull its
 *     district list. Triggered when the user picks a province; clears wards.
 *   – {@link selectDistrict} re-fetches the district with depth=2 to pull its
 *     ward list. Triggered when the user picks a district.
 *
 * The names returned mirror the GHTK directory (e.g. "Hồ Chí Minh", "Quận 1",
 * "Phường Bến Nghé") so the fee endpoint accepts them verbatim — we strip the
 * leading "Thành phố " / "Tỉnh " prefix from province names to match.
 */

interface Province { code: number; name: string }
interface District { code: number; name: string }
interface Ward     { code: number; name: string }

const API = "https://provinces.open-api.vn/api";

let provincesCache: Province[] | null = null;

function stripPrefix(name: string): string {
  return name.replace(/^Thành phố\s+/, "").replace(/^Tỉnh\s+/, "");
}

export function useVnAddress() {
  const [provinces, setProvinces] = useState<Province[]>([]);
  const [districts, setDistricts] = useState<District[]>([]);
  const [wards, setWards] = useState<Ward[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (provincesCache) { setProvinces(provincesCache); return; }
      try {
        const res = await fetch(`${API}/p/`);
        const list = (await res.json()) as Province[];
        const cleaned = list
          .map((p) => ({ code: p.code, name: stripPrefix(p.name) }))
          .sort((a, b) => a.name.localeCompare(b.name, "vi"));
        provincesCache = cleaned;
        if (!cancelled) setProvinces(cleaned);
      } catch {
        if (!cancelled) setProvinces([]);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const selectProvince = useCallback(async (code: number | null) => {
    setDistricts([]);
    setWards([]);
    if (code == null) return;
    setLoading(true);
    try {
      const res = await fetch(`${API}/p/${code}?depth=2`);
      const data = await res.json();
      const list = (data.districts ?? []) as District[];
      setDistricts(list.sort((a, b) => a.name.localeCompare(b.name, "vi")));
    } catch {
      setDistricts([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const selectDistrict = useCallback(async (code: number | null) => {
    setWards([]);
    if (code == null) return;
    setLoading(true);
    try {
      const res = await fetch(`${API}/d/${code}?depth=2`);
      const data = await res.json();
      const list = (data.wards ?? []) as Ward[];
      setWards(list.sort((a, b) => a.name.localeCompare(b.name, "vi")));
    } catch {
      setWards([]);
    } finally {
      setLoading(false);
    }
  }, []);

  return { provinces, districts, wards, loading, selectProvince, selectDistrict };
}
