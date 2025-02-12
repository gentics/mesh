export interface GenericErrorResponse {
    type: string;
    message: string;
    properties: Record<string, string>;
    i18nKey?: string;
    i18nParameters?: string[];
}
