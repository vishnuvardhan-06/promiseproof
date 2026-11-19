import { useContext } from "react";

import { AuthContext } from "./authContext";

// Access the signed-in user and the authentication actions available in the
// application.
export function useAuth() {
    return useContext(AuthContext);
}
