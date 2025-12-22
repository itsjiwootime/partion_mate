/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        mint: {
          50: '#effef9',
          100: '#c9f7ea',
          200: '#9feeda',
          300: '#6be5c7',
          400: '#3fd6b3',
          500: '#2ad3b5',
          600: '#1aa58c',
          700: '#12806d',
          800: '#0f6758',
          900: '#0b4d42',
        },
        ink: '#0f2c2a',
        'clean-white': '#f7faf9',
      },
    },
  },
  plugins: [],
};
