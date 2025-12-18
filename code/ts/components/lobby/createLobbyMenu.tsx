import React, { useState } from 'react';
import '../../styles/lobbies.css';

interface CreateLobbyFormData {
    name: string;
    description: string;
    minPlayers: number;
    maxPlayers: number;
}

interface CreateLobbyMenuProps {
    isOpen: boolean;
    onClose: () => void;
    onCreate: (data: CreateLobbyFormData) => Promise<void>;
    isCreating: boolean;
}

export function CreateLobbyMenu({ isOpen, onClose, onCreate, isCreating }: CreateLobbyMenuProps) {
    const [formData, setFormData] = useState<CreateLobbyFormData>({
        name: '',
        description: '',
        minPlayers: 2,
        maxPlayers: 4,
    });
    const [error, setError] = useState<string | null>(null);

    const handleCreateFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === 'minPlayers' || name === 'maxPlayers' ? parseInt(value) || 0 : value,
        }));
        setError(null); // Clear error on change
    };

    const handleSubmit = async () => {
        // Validation Logic
        if (!formData.name.trim()) {
            setError('Lobby name is required');
            return;
        }

        if (formData.minPlayers < 2 || formData.minPlayers > 10) {
            setError('Min players must be between 2 and 10');
            return;
        }

        if (formData.maxPlayers < 2 || formData.maxPlayers > 10) {
            setError('Max players must be between 2 and 10');
            return;
        }

        if (formData.minPlayers > formData.maxPlayers) {
            setError('Min players cannot be greater than max players');
            return;
        }

        await onCreate(formData);
        if (!isCreating) {
             setFormData({ name: '', description: '', minPlayers: 2, maxPlayers: 4 });
        }
    };

    return (
        <>
            {isOpen && (
                <div
                    className="lobby-menu-overlay"
                    onClick={onClose}
                />
            )}

            <div className={`lobby-create-menu ${isOpen ? 'open' : ''}`}>
                <div className="lobby-create-menu-header">
                    <h2 className="lobby-create-menu-title">Create New Lobby</h2>
                    <button
                        onClick={onClose}
                        className="lobby-create-menu-close"
                    >
                        ✕
                    </button>
                </div>

                <div className="lobby-create-form">
                    {error && <div className="lobby-error" style={{ marginBottom: '1rem' }}>{error}</div>}

                    <div className="lobby-form-group">
                        <label htmlFor="name" className="lobby-form-label">
                            Lobby Name *
                        </label>
                        <textarea
                            id="name"
                            name="name"
                            value={formData.name}
                            onChange={handleCreateFormChange}
                            rows={1}
                            className="lobby-form-textarea"
                            placeholder="Enter lobby name"
                        />
                    </div>

                    <div className="lobby-form-group">
                        <label htmlFor="description" className="lobby-form-label">
                            Description
                        </label>
                        <textarea
                            id="description"
                            name="description"
                            value={formData.description}
                            onChange={handleCreateFormChange}
                            rows={4}
                            className="lobby-form-textarea"
                            placeholder="Optional description"
                        />
                    </div>

                    <div className="lobby-form-group">
                        <label htmlFor="minPlayers" className="lobby-form-label">
                            Min Players
                        </label>
                        <input
                            type="number"
                            id="minPlayers"
                            name="minPlayers"
                            value={formData.minPlayers}
                            onChange={handleCreateFormChange}
                            min="2"
                            max="10"
                            className="lobby-form-input"
                        />
                    </div>

                    <div className="lobby-form-group">
                        <label htmlFor="maxPlayers" className="lobby-form-label">
                            Max Players
                        </label>
                        <input
                            type="number"
                            id="maxPlayers"
                            name="maxPlayers"
                            value={formData.maxPlayers}
                            onChange={handleCreateFormChange}
                            min="2"
                            max="10"
                            className="lobby-form-input"
                        />
                    </div>

                    <div className="lobby-form-hint">
                        Players must be between 2 and 10
                    </div>

                    <div className="lobby-create-actions">
                        <button
                            onClick={handleSubmit}
                            className="lobby-create-button"
                            disabled={isCreating}
                        >
                            {isCreating ? 'Creating...' : 'Create Lobby'}
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
}
