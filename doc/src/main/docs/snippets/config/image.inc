
[options="header"]
|======
| Configuration              | Type   | Default   | Description 
| ```image.maxWidth```       | Number | `2048`    | The maximum allowed image resize width. Resizing is a memory intensive operation and thus this limit can help avoid memory issues.
| ```image.maxHeight```      | Number | `2048`    | The maximum allowed image resize height. Resizing is a memory intensive operation and thus this limit can help avoid memory issues.
| ```image.jpegQuality```    | Number | `0.95`    | Configure the quality of the output of JPEG images. Must be a value between inclusive 0 and inclusive 1.
| ```image.resampleFilter``` | String | `LANCZOS` | Configure the filter that is used when resizing images.
|======

Filters:

* `UNDEFINED` - Undefined interpolation, filter method will use default filter.
* `POINT`     - Point interpolation (also known as "nearest neighbour"). Very fast, but low quality
* `BOX`       - Box interpolation. Fast, but low quality.
* `TRIANGLE`  - Triangle interpolation (also known as "linear" or "bilinear"). Quite fast, with acceptable quality
* `HERMITE`   - Hermite interpolation.
* `HANNING`   - Hanning interpolation.
* `HAMMING`   - Hamming interpolation.
* `BLACKMAN`  - Blackman interpolation..
* `GAUSSIAN`  - Gaussian interpolation.
* `QUADRATIC` - Quadratic interpolation.
* `CUBIC`     - Cubic interpolation.
* `CATROM`    - Catrom interpolation.
* `MITCHELL`  - Mitchell interpolation. High quality.
* `LANCZOS`   - Lanczos interpolation. High quality.
* `BLACKMAN_BESSEL` - Blackman-Bessel interpolation. High quality.
* `BLACKMAN_SINC`   - Blackman-Sinc interpolation. High quality.