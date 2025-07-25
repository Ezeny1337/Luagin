import { useEffect, useRef, useState } from "react";

export function usePerfData() {
  const [data, setData] = useState<any>(null);
  const timer = useRef<any>(null);

  useEffect(() => {
    async function fetchData() {
      try {
        const res = await fetch("/api/perf/all", {
          credentials: 'include',
        });
        if (res.ok) {
          const json = await res.json();
          setData(json.data);
        }
      } catch (e) {
      }
    }
    fetchData();
    timer.current = setInterval(fetchData, 1000);
    return () => clearInterval(timer.current);
  }, []);

  return data;
} 