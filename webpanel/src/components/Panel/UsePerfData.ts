import { useEffect, useRef, useState } from "react";

export function usePerfData() {
  const [data, setData] = useState<any>(null);
  const [enabled, setEnabled] = useState<boolean | null>(null); // null表示加载中
  const timer = useRef<any>(null);

  useEffect(() => {
    async function fetchData() {
      try {
        const res = await fetch("/api/perf/all", {
          credentials: 'include',
        });
        if (res.ok) {
          const json = await res.json();
          setEnabled(json.enabled !== false);
          setData(json.data);
        }
      } catch (e) {
        // 发生错误时，假设未启用
        setEnabled(false);
        setData(null);
      }
    }
    fetchData();
    timer.current = setInterval(fetchData, 1000);
    return () => clearInterval(timer.current);
  }, []);

  return { data, enabled };
}