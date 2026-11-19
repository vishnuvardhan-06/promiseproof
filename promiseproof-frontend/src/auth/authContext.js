import { createContext } from "react";

// Shared auth context instance. Kept in its own module so that the context
// object and the provider component can be separated for React fast refresh.
export const AuthContext = createContext(null);
