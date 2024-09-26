export interface BinaryFieldTransformRequest {
    /** Crop mode. To be used in conjunction with cropRect */
    cropMode?: string;
    /** Crop area. */
    cropRect?: ImageRect;
    /** Optional new focal point for the transformed image. */
    focalPoint?: FocalPoint;
    /** New height of the image. */
    height?: number;
    /**
     * ISO 639-1 language tag of the node which provides the image which should be
     * transformed.
     */
    language: string;
    /** Resize mode. */
    resizeMode?: string;
    /**
     * Version number which must be provided in order to handle and detect concurrent
     * changes to the node content.
     */
    version: string;
    /** New width of the image. */
    width?: number;
}

/** Crop area. */
export interface ImageRect {
    height?: number;
    startX?: number;
    startY?: number;
    width?: number;
}

/** Optional new focal point for the transformed image. */
export interface FocalPoint {
    /**
     * The horizontal position of the focal point. The value is a factor of the image
     * width. The value 0.5 is the center of the image.
     */
    x?: number;
    /**
     * The vertical position of the focal point. The value is a factor of the image
     * height. The value 0.5 is the center of the image.
     */
    y?: number;
}
