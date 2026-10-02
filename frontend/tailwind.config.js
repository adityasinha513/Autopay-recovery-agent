/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#17212f',
        muted: '#718096',
        line: '#e8edf2',
        canvas: '#f5f7fa',
        brand: '#245c4c',
        mint: '#e6f3ed',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 10px 30px rgba(26, 39, 52, 0.045)',
      },
    },
  },
  plugins: [],
}
