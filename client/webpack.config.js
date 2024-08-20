const path = require('path');
const Dotenv = require('dotenv-webpack');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = (env, argv) => {
    // Determine the mode (production or development)
    const isProduction = argv.mode === 'production';

    return {
        mode: isProduction ? 'production' : 'development',
        entry: './src/index.js',
        resolve: {
            extensions: ['.ts', '.tsx', '.js'],
            fallback: {
                "fs": false,
                "tls": false,
                "net": false,
                "path": false,
                "zlib": false,
                "http": false,
                "https": false,
                "stream": false,
                "crypto": false,
            }
        },
        module: {
            rules: [
                {
                    test: /\.(js|ts|tsx)$/,
                    exclude: /node_modules/,
                    use: 'babel-loader',
                },
                {
                    test: /\.css$/,
                    use: [MiniCssExtractPlugin.loader, 'css-loader'],
                },
                {
                    test: /\.(png|jpg|gif|svg)$/,
                    use: [
                        {
                            loader: 'file-loader',
                            options: {
                                name: '[name].[hash].[ext]',
                                outputPath: 'assets/images',
                            },
                        },
                    ]
                }
            ],
        },
        devServer: {
            static: path.join(__dirname, 'dist'),
            historyApiFallback: true,
            compress: true,
            port: 3000,
        },
        output: {
            filename: 'bundle.js',
            path: path.resolve(__dirname, 'dist'),
            publicPath: '/'
        },
        plugins: [
            new Dotenv({
                path: isProduction
                    ? path.resolve(__dirname, '.env.production')
                    : path.resolve(__dirname, '.env.development'),
            }),
            new MiniCssExtractPlugin(),
            new HtmlWebpackPlugin({
                filename: "index.html",
                template: path.resolve(__dirname, "./public/index.html"),
                favicon: path.resolve(__dirname, "./public/favicon.ico")
            })
        ]
    };
};
