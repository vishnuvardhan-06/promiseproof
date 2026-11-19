import { useCallback, useEffect, useState } from "react";

import * as authApi from "../services/authApi";

import { AuthContext } from "./authContext";

export function AuthProvider({ children }) {

    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {

        try {
            const currentUser = await authApi.getMe();
            setUser(currentUser);
        } catch {
            setUser(null);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {

        let cancelled = false;

        async function loadSession() {

            await refresh();

            if (cancelled) {
                return;
            }
        }

        loadSession();

        return () => {
            cancelled = true;
        };
    }, [refresh]);

    async function login(email, password) {

        const currentUser = await authApi.login(email, password);
        setUser(currentUser);
        return currentUser;
    }

    async function register(details) {

        const created = await authApi.register(details);

        // Sign the new citizen in straight away.
        const currentUser = await authApi.login(
            details.email,
            details.password
        );

        setUser(currentUser);
        return created;
    }

    async function logout() {

        try {
            await authApi.logout();
        } finally {
            setUser(null);
        }
    }

    function hasRole(...roles) {
        return Boolean(user && roles.includes(user.role));
    }

    const value = {
        user,
        loading,
        login,
        register,
        logout,
        refresh,
        hasRole
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}
