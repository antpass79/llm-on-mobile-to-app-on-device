import { create } from 'zustand';

export interface ImageParameters {
  gain: number;
  depth: number;
  zoom: number;
}

interface ImageParametersState extends ImageParameters {
  setGain: (gain: number) => void;
  setDepth: (depth: number) => void;
  setZoom: (zoom: number) => void;
  setParameters: (params: Partial<ImageParameters>) => void;
}

export const useImageParametersStore = create<ImageParametersState>((set) => ({
  gain: 50,
  depth: 100,
  zoom: 1,
  setGain: (gain) => set({ gain }),
  setDepth: (depth) => set({ depth }),
  setZoom: (zoom) => set({ zoom }),
  setParameters: (params) => set((state) => ({ ...state, ...params })),
}));
