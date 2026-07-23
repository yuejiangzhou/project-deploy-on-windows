/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: 'var(--color-primary)',
        'primary-hover': 'var(--color-primary-hover)',
        'primary-light': 'var(--color-primary-light)',
        'bg-page': 'var(--color-bg)',
        'bg-elevated': 'var(--color-bg-elevated)',
        'bg-sunken': 'var(--color-bg-sunken)',
        'border': 'var(--color-border)',
        'border-light': 'var(--color-border-light)',
        'text-primary': 'var(--color-text-primary)',
        'text-secondary': 'var(--color-text-secondary)',
        'text-tertiary': 'var(--color-text-tertiary)',
        'state-success': 'var(--state-success)',
        'state-warning': 'var(--state-warning)',
        'state-error': 'var(--state-error)',
        'state-info': 'var(--state-info)',
      },
      borderRadius: {
        'md': 'var(--radius-md)',
        'lg': 'var(--radius-lg)',
        'full': 'var(--radius-full)',
      },
      fontFamily: {
        sans: 'var(--font-family)',
        mono: 'var(--font-mono)',
      },
      boxShadow: {
        'sm': 'var(--shadow-sm)',
        'md': 'var(--shadow-md)',
        'float': 'var(--shadow-float)',
      }
    },
  },
  plugins: [],
}
