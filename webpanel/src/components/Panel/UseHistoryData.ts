import { useRef } from 'react';

export function useHistoryData<T>(value: T, maxLen = 60) {
  const ref = useRef<T[]>([]);
  if (ref.current.length === 0 || ref.current[ref.current.length - 1] !== value) {
    ref.current.push(value);
    if (ref.current.length > maxLen) ref.current.shift();
  }
  return ref.current.slice();
} 