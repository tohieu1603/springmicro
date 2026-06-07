// SVG Illustration Library - hand-drawn style luxury items
// Generates SVG data URIs that can be used as CSS background-image.

export type Palette = {
  bg1: string;
  bg2: string;
  accent: string;
  name: string;
};

export const PALETTES: Palette[] = [
  { bg1: '#5a1f24', bg2: '#2b0d12', accent: '#d4a574', name: 'wine' },
  { bg1: '#1f4d3a', bg2: '#0a2618', accent: '#e3c08d', name: 'forest' },
  { bg1: '#7a1a26', bg2: '#2e0810', accent: '#f0d090', name: 'crimson' },
  { bg1: '#1a3a52', bg2: '#0a1c2a', accent: '#d8b878', name: 'navy' },
  { bg1: '#3a3a3a', bg2: '#0c0c0c', accent: '#c89860', name: 'graphite' },
  { bg1: '#4d3a1a', bg2: '#1a1208', accent: '#f0c87a', name: 'amber' },
  { bg1: '#2a4d4d', bg2: '#0a1a1a', accent: '#d8b870', name: 'teal' },
  { bg1: '#5c2a3e', bg2: '#1c0a14', accent: '#e0b890', name: 'rose' },
];

export type IllustStyle =
  | 'topHandle'
  | 'hobo'
  | 'chevron'
  | 'bucket'
  | 'shoe'
  | 'wallet'
  | 'model';

function svgDataUri(svg: string): string {
  return `url("data:image/svg+xml;utf8,${encodeURIComponent(svg)}")`;
}

function bagTopHandle(p: Palette): string {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 1500' preserveAspectRatio='xMidYMid slice'>
    <defs>
      <radialGradient id='b${p.name}1' cx='50%' cy='40%' r='80%'>
        <stop offset='0%' stop-color='${p.bg1}'/>
        <stop offset='100%' stop-color='${p.bg2}'/>
      </radialGradient>
    </defs>
    <rect width='1200' height='1500' fill='url(%23b${p.name}1)'/>
    <g transform='translate(600 780)' fill='none' stroke='${p.accent}' stroke-width='4' stroke-linejoin='round' stroke-linecap='round'>
      <path d='M-180 -360 C -180 -480, 180 -480, 180 -360' stroke-width='8'/>
      <path d='M-340 -340 L-380 280 Q-380 340 -320 340 L320 340 Q380 340 380 280 L340 -340 Z' fill='${p.accent}' fill-opacity='0.92' stroke='${p.accent}'/>
      <path d='M-340 -340 L340 -340 L320 0 L-320 0 Z' fill='${p.bg1}' fill-opacity='0.4' stroke='${p.accent}' stroke-width='3'/>
      <rect x='-40' y='-340' width='80' height='680' fill='${p.bg1}' fill-opacity='0.55'/>
      <rect x='-10' y='-340' width='20' height='680' fill='${p.accent}' fill-opacity='0.4'/>
      <rect x='-40' y='-30' width='80' height='110' rx='4' fill='${p.bg2}' stroke='${p.accent}' stroke-width='2'/>
      <circle cx='0' cy='25' r='14' fill='none' stroke='${p.accent}' stroke-width='3'/>
    </g>
  </svg>`;
}

function bagHobo(p: Palette): string {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 1500' preserveAspectRatio='xMidYMid slice'>
    <defs>
      <radialGradient id='h${p.name}' cx='50%' cy='45%' r='85%'>
        <stop offset='0%' stop-color='${p.bg1}'/>
        <stop offset='100%' stop-color='${p.bg2}'/>
      </radialGradient>
      <pattern id='fl${p.name}' x='0' y='0' width='90' height='90' patternUnits='userSpaceOnUse'>
        <circle cx='20' cy='25' r='6' fill='${p.accent}' opacity='0.55'/>
        <circle cx='60' cy='55' r='4' fill='${p.accent}' opacity='0.4'/>
        <circle cx='40' cy='75' r='5' fill='${p.accent}' opacity='0.5'/>
        <path d='M70 15 q5 5 0 10 q-5 -5 0 -10' fill='${p.accent}' opacity='0.45'/>
      </pattern>
    </defs>
    <rect width='1200' height='1500' fill='url(%23h${p.name})'/>
    <g transform='translate(600 760)' fill='none' stroke='${p.accent}' stroke-width='4' stroke-linejoin='round'>
      <path d='M-220 -240 C -260 -440, 260 -440, 220 -240' stroke-width='6'/>
      <path d='M-380 -240 C -460 0, -380 300, -120 320 L 120 320 C 380 300, 460 0, 380 -240 Z' fill='#f0e8d8' stroke='${p.accent}' stroke-width='3'/>
      <path d='M-380 -240 C -460 0, -380 300, -120 320 L 120 320 C 380 300, 460 0, 380 -240 Z' fill='url(%23fl${p.name})' opacity='0.9'/>
      <rect x='-60' y='-240' width='120' height='560' fill='#2d6a3a' opacity='0.75'/>
      <rect x='-18' y='-240' width='36' height='560' fill='#b8252a' opacity='0.85'/>
      <g transform='translate(0 30)' stroke='${p.accent}' stroke-width='3'>
        <circle cx='-18' cy='0' r='22' fill='none'/>
        <circle cx='18' cy='0' r='22' fill='none'/>
      </g>
    </g>
  </svg>`;
}

function bagChevron(p: Palette): string {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 1500' preserveAspectRatio='xMidYMid slice'>
    <defs>
      <linearGradient id='c${p.name}' x1='0%' y1='0%' x2='0%' y2='100%'>
        <stop offset='0%' stop-color='${p.bg1}'/>
        <stop offset='100%' stop-color='${p.bg2}'/>
      </linearGradient>
    </defs>
    <rect width='1200' height='1500' fill='url(%23c${p.name})'/>
    <g transform='translate(600 780)' fill='none' stroke='${p.accent}' stroke-linejoin='round'>
      <path d='M-380 -260 Q -200 -380, 0 -340 Q 200 -380, 380 -260' stroke='${p.accent}' stroke-width='4' fill='none'/>
      <g stroke='${p.accent}' stroke-width='3' fill='${p.accent}' fill-opacity='0.4'>
        <ellipse cx='-340' cy='-280' rx='10' ry='6'/>
        <ellipse cx='-260' cy='-320' rx='10' ry='6'/>
        <ellipse cx='-180' cy='-350' rx='10' ry='6'/>
        <ellipse cx='-100' cy='-345' rx='10' ry='6'/>
        <ellipse cx='100' cy='-345' rx='10' ry='6'/>
        <ellipse cx='180' cy='-350' rx='10' ry='6'/>
        <ellipse cx='260' cy='-320' rx='10' ry='6'/>
        <ellipse cx='340' cy='-280' rx='10' ry='6'/>
      </g>
      <rect x='-380' y='-260' width='760' height='480' rx='30' fill='${p.bg2}' stroke='${p.accent}' stroke-width='4'/>
      <g stroke='${p.accent}' stroke-width='2' opacity='0.55' fill='none'>
        <path d='M-360 -200 L 0 -70 L 360 -200'/>
        <path d='M-360 -130 L 0 0 L 360 -130'/>
        <path d='M-360 -60 L 0 70 L 360 -60'/>
        <path d='M-360 10 L 0 140 L 360 10'/>
      </g>
      <g transform='translate(0 0)' stroke='${p.accent}' stroke-width='6' fill='none'>
        <path d='M-50 -40 a 40 40 0 1 0 40 40 L -10 0'/>
        <path d='M 10 -40 a 40 40 0 1 1 -40 40 L 0 0'/>
      </g>
    </g>
  </svg>`;
}

function bagBucket(p: Palette): string {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 1500' preserveAspectRatio='xMidYMid slice'>
    <defs>
      <radialGradient id='bu${p.name}' cx='50%' cy='40%' r='80%'>
        <stop offset='0%' stop-color='${p.bg1}'/>
        <stop offset='100%' stop-color='${p.bg2}'/>
      </radialGradient>
    </defs>
    <rect width='1200' height='1500' fill='url(%23bu${p.name})'/>
    <g transform='translate(600 800)' fill='none' stroke='${p.accent}' stroke-linejoin='round'>
      <path d='M-180 -380 Q -180 -480, -100 -480 L -100 -380' stroke='${p.accent}' stroke-width='6'/>
      <path d='M 180 -380 Q 180 -480, 100 -480 L 100 -380' stroke='${p.accent}' stroke-width='6'/>
      <path d='M-340 -380 L 340 -380 L 400 320 L -400 320 Z' fill='${p.accent}' fill-opacity='0.18' stroke='${p.accent}' stroke-width='3'/>
      <path d='M-400 280 L 400 280 L 400 320 L -400 320 Z' fill='${p.bg2}' opacity='0.5'/>
      <rect x='-50' y='-380' width='100' height='700' fill='${p.bg1}' opacity='0.55'/>
      <rect x='-12' y='-380' width='24' height='700' fill='${p.accent}' opacity='0.6'/>
    </g>
  </svg>`;
}

function shoeHeel(p: Palette): string {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 1500' preserveAspectRatio='xMidYMid slice'>
    <defs>
      <radialGradient id='ovr${p.name}' cx='50%' cy='50%' r='70%'>
        <stop offset='0%' stop-color='transparent'/>
        <stop offset='100%' stop-color='${p.bg2}'/>
      </radialGradient>
    </defs>
    <rect width='1200' height='1500' fill='${p.bg1}'/>
    <rect width='1200' height='1500' fill='url(%23ovr${p.name})' opacity='0.4'/>
    <g transform='translate(600 800)' fill='none' stroke='${p.accent}' stroke-width='5' stroke-linejoin='round' stroke-linecap='round'>
      <path d='M-300 100 Q -340 -20, -260 -60 Q -100 -120, 80 -80 L 280 -60 Q 320 -50, 320 -10 L 300 30 Q 280 60, 200 80 L -200 110 Q -280 130, -300 100 Z' fill='${p.accent}' fill-opacity='0.85'/>
      <path d='M 240 80 L 260 80 L 200 240 L 180 240 Z' fill='${p.accent}'/>
      <path d='M-80 -90 Q 0 -130, 80 -90' stroke='${p.bg2}' stroke-width='6'/>
    </g>
  </svg>`;
}

function walletSmall(p: Palette): string {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 1500' preserveAspectRatio='xMidYMid slice'>
    <defs>
      <radialGradient id='wg${p.name}' cx='50%' cy='50%' r='80%'>
        <stop offset='0%' stop-color='transparent'/>
        <stop offset='100%' stop-color='${p.bg2}'/>
      </radialGradient>
    </defs>
    <rect width='1200' height='1500' fill='${p.bg1}'/>
    <rect x='0' y='0' width='1200' height='1500' fill='url(%23wg${p.name})'/>
    <g transform='translate(600 800)' fill='none' stroke='${p.accent}' stroke-width='4' stroke-linejoin='round'>
      <rect x='-260' y='-180' width='520' height='320' rx='14' fill='${p.accent}' fill-opacity='0.92'/>
      <line x1='-260' y1='-20' x2='260' y2='-20' stroke='${p.bg2}' stroke-width='3' opacity='0.5'/>
      <rect x='-40' y='-180' width='80' height='320' fill='${p.bg1}' opacity='0.6'/>
      <rect x='-10' y='-180' width='20' height='320' fill='${p.bg2}' opacity='0.7'/>
      <circle cx='-150' cy='-70' r='8' fill='${p.bg2}' opacity='0.5'/>
      <circle cx='150' cy='-70' r='8' fill='${p.bg2}' opacity='0.5'/>
    </g>
  </svg>`;
}

function modelWithBag(p: Palette): string {
  return `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 1500' preserveAspectRatio='xMidYMid slice'>
    <defs>
      <linearGradient id='m${p.name}' x1='0%' y1='0%' x2='0%' y2='100%'>
        <stop offset='0%' stop-color='${p.bg1}'/>
        <stop offset='100%' stop-color='${p.bg2}'/>
      </linearGradient>
    </defs>
    <rect width='1200' height='1500' fill='url(%23m${p.name})'/>
    <g transform='translate(600 0)' fill='${p.accent}' fill-opacity='0.18' stroke='${p.accent}' stroke-width='3'>
      <circle cx='0' cy='200' r='80'/>
      <path d='M-25 260 L-30 320 L30 320 L25 260 Z'/>
      <path d='M-160 320 Q -200 380, -180 460 L-200 900 L-100 1500 L100 1500 L200 900 L180 460 Q 200 380, 160 320 L 80 300 L 30 320 L -30 320 L -80 300 Z' fill='${p.bg1}' fill-opacity='0.7'/>
      <path d='M160 380 Q 280 500, 260 700' stroke='${p.accent}' stroke-width='14' fill='none' stroke-linecap='round'/>
    </g>
    <g transform='translate(860 760)' fill='none' stroke='${p.accent}' stroke-width='3' stroke-linejoin='round'>
      <path d='M-100 -80 C -110 60, 100 70, 100 -80 Z' fill='#f0e8d8' fill-opacity='0.92'/>
      <rect x='-15' y='-80' width='30' height='150' fill='${p.bg1}' opacity='0.7'/>
      <rect x='-5' y='-80' width='10' height='150' fill='${p.accent}' opacity='0.6'/>
      <path d='M-60 -90 Q 0 -130, 60 -90' stroke='${p.accent}' stroke-width='4' fill='none'/>
    </g>
  </svg>`;
}

const STYLE_FNS: Record<IllustStyle, (p: Palette) => string> = {
  topHandle: bagTopHandle,
  hobo: bagHobo,
  chevron: bagChevron,
  bucket: bagBucket,
  shoe: shoeHeel,
  wallet: walletSmall,
  model: modelWithBag,
};

export function makeBg(style: IllustStyle, paletteIdx: number): string {
  const p = PALETTES[paletteIdx % PALETTES.length];
  const fn = STYLE_FNS[style] ?? bagHobo;
  return svgDataUri(fn(p));
}
