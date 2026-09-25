import { HttpErrorResponse } from '@angular/common/http';
import { ApiError, RequestError } from '../models/api.models';

function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'code' in value &&
    typeof value.code === 'string' &&
    'message' in value &&
    typeof value.message === 'string'
  );
}

export function toRequestError(error: unknown): RequestError {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return {
        message: "Impossible de joindre le serveur. Vérifiez votre connexion puis réessayez.",
        network: true,
      };
    }

    if (isApiError(error.error)) {
      return { code: error.error.code, message: error.error.message, network: false };
    }
  }

  return {
    message: "Une erreur inattendue est survenue. Veuillez réessayer.",
    network: false,
  };
}
