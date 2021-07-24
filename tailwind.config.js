const defaultTheme = require('tailwindcss/defaultTheme')

module.exports = {
    mode: 'jit',
    purge: [
        'src/cljs/**/*.cljs',
        'src/clj/**/*.clj',
    ],
    darkMode: false, // or 'media' or 'class'
    theme: {
        extend: {
            fontFamily: {
                sans: ["Inter var", ...defaultTheme.fontFamily.sans],
            },
        },
        container: {
            center: true,
            padding: '2rem',
        },
    },
    variants: {},
    plugins: [
        require('@tailwindcss/forms'),
    ],
}
