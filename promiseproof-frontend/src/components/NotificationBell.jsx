import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
    getNotifications,
    markAllNotificationsRead,
    markNotificationRead
} from "../services/notificationApi";

function formatRelative(value) {

    if (!value) {
        return "";
    }

    const then = new Date(value).getTime();
    const seconds = Math.max(0, Math.round((Date.now() - then) / 1000));

    if (seconds < 60) {
        return "just now";
    }

    const minutes = Math.round(seconds / 60);

    if (minutes < 60) {
        return `${minutes}m ago`;
    }

    const hours = Math.round(minutes / 60);

    if (hours < 24) {
        return `${hours}h ago`;
    }

    const days = Math.round(hours / 24);

    if (days < 7) {
        return `${days}d ago`;
    }

    return new Date(value).toLocaleDateString();
}

function NotificationBell() {

    const navigate = useNavigate();

    const [open, setOpen] = useState(false);
    const [items, setItems] = useState([]);
    const [unread, setUnread] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const wrapperRef = useRef(null);

    const load = useCallback(async (showSpinner = false) => {

        try {

            if (showSpinner) {
                setLoading(true);
            }

            const data = await getNotifications();

            setItems(data.items || []);
            setUnread(data.unreadCount || 0);
            setError("");

        } catch (err) {

            setError(err.message);

        } finally {

            if (showSpinner) {
                setLoading(false);
            }
        }
    }, []);

    useEffect(() => {

        let cancelled = false;

        async function refresh() {

            await load(false);

            if (cancelled) {
                return;
            }
        }

        refresh();

        const timer = setInterval(() => load(false), 60000);

        return () => {

            cancelled = true;
            clearInterval(timer);
        };

    }, [load]);

    useEffect(() => {

        function handleClickOutside(event) {

            if (wrapperRef.current &&
                !wrapperRef.current.contains(event.target)) {
                setOpen(false);
            }
        }

        document.addEventListener("mousedown", handleClickOutside);

        return () =>
            document.removeEventListener("mousedown", handleClickOutside);

    }, []);

    async function handleToggle() {

        const next = !open;

        setOpen(next);

        if (next) {
            load(true);
        }
    }

    async function handleOpenItem(item) {

        try {

            if (!item.read) {

                await markNotificationRead(item.id);

                setItems(previous =>
                    previous.map(entry =>
                        entry.id === item.id
                            ? { ...entry, read: true }
                            : entry
                    )
                );

                setUnread(current => Math.max(0, current - 1));
            }

            setOpen(false);

            if (item.link) {
                navigate(item.link);
            }

        } catch (err) {
            setError(err.message);
        }
    }

    async function handleMarkAll() {

        try {

            await markAllNotificationsRead();

            setItems(previous =>
                previous.map(entry => ({ ...entry, read: true }))
            );

            setUnread(0);

        } catch (err) {
            setError(err.message);
        }
    }

    return (

        <div className="notification-bell" ref={wrapperRef}>

            <button
                type="button"
                className="notification-trigger"
                onClick={handleToggle}
                aria-label={
                    unread > 0
                        ? `Notifications, ${unread} unread`
                        : "Notifications"
                }
            >
                <span aria-hidden="true">🔔</span>
                {unread > 0 && (
                    <span className="notification-badge">
                        {unread > 9 ? "9+" : unread}
                    </span>
                )}
            </button>

            {open && (

                <div className="notification-panel">

                    <div className="notification-head">
                        <strong>Notifications</strong>
                        <button
                            type="button"
                            className="link-button"
                            onClick={handleMarkAll}
                            disabled={unread === 0}
                        >
                            Mark all read
                        </button>
                    </div>

                    {error && (
                        <p className="notification-error">{error}</p>
                    )}

                    {loading ? (

                        <p className="notification-empty">Loading...</p>

                    ) : items.length === 0 ? (

                        <p className="notification-empty">
                            Nothing here yet. Review decisions and new
                            submissions will appear here.
                        </p>

                    ) : (

                        <ul className="notification-list">
                            {items.map(item => (
                                <li key={item.id}>
                                    <button
                                        type="button"
                                        className={
                                            "notification-item" +
                                            (item.read ? "" : " unread")
                                        }
                                        onClick={() => handleOpenItem(item)}
                                    >
                                        <span className="notification-title">
                                            {item.title}
                                        </span>
                                        {item.message && (
                                            <span className="notification-message">
                                                {item.message}
                                            </span>
                                        )}
                                        <span className="notification-time">
                                            {formatRelative(item.createdAt)}
                                        </span>
                                    </button>
                                </li>
                            ))}
                        </ul>

                    )}

                </div>
            )}

        </div>
    );
}

export default NotificationBell;
