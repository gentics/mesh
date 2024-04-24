export function toRelativePath(path: string): string {
    if (!path.startsWith('/')) {
        return `/${path}`;
    }
    return path;
}

export function trimTrailingSlash(path: string): string {
    if (!path.endsWith('/')) {
        return path;
    }
    return path.substring(0, path.length - 1);
}
